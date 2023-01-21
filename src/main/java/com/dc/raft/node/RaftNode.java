package com.dc.raft.node;

import com.dc.raft.LifeCycle;
import com.dc.raft.RequestClients;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.command.heartbeat.HeartBeatRequest;
import com.dc.raft.command.heartbeat.HeartBeatResponse;
import com.dc.raft.rpc.GrpcClient;
import com.dc.raft.store.log.LogStore;
import com.dc.raft.store.log.RocketDbLogStore;
import com.dc.raft.store.statemachine.DataStateMachine;
import com.dc.raft.command.vote.VoteRequest;
import com.dc.raft.command.vote.VoteResponse;
import com.dc.raft.store.statemachine.RocksStateMachine;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RaftNode implements LifeCycle {

    //默认的超时选举时间为 15s
    private static final long DEFAULT_ELECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(15L);

    private static final long SCHEDULE_PERIOD_TIME = TimeUnit.MILLISECONDS.toMillis(500);

    private final AtomicBoolean START_STATE = new AtomicBoolean(false);

    private ScheduledExecutorService executorService;

    /**
     * 接受心跳任务
     */
    HeartBeakTask heartBeakTask = new HeartBeakTask();


    /**
     * 用于超时发起选举
     */
    ElectionTask electionTask = new ElectionTask();


    /**
     * 所有的Raft节点信息
     */
    @Setter
    @Getter
    private RaftPeers raftPeers;

    /**
     * 当前节点的状态
     */
    private NodeStatus nodeStatus = NodeStatus.FOLLOWER;

    /**
     * 服务器最后一次知道的任期号（初始化为 0，持续递增）
     */
    private final AtomicLong currentTerm = new AtomicLong();

    /**
     * 在当前获得选票的候选人的 Id
     */
    volatile RaftPeers.PeerNode votedFor;

    /**
     * 日志存储
     */
    @Getter
    private LogStore logStore;

    /**
     * 复制状态机
     */
    @Getter
    private DataStateMachine stateMachine;


    public RaftNode() {

    }

    @Override
    public void start() throws Exception {
        if (START_STATE.compareAndSet(false, true)) {
            executorService = Executors.newScheduledThreadPool(2);
            //添加选举超时任务,每500ms执行一次
            executorService.scheduleWithFixedDelay(electionTask, 0, SCHEDULE_PERIOD_TIME, TimeUnit.MILLISECONDS);

            //添加心跳定时任务, 每500ms执行一次
            executorService.scheduleWithFixedDelay(heartBeakTask, 0, SCHEDULE_PERIOD_TIME, TimeUnit.MILLISECONDS);
            log.info("Raft peer start {}", raftPeers.getSelf());

            //用于存储日志索引
            logStore = new RocketDbLogStore(raftPeers.getSelf());

            //状态机 用于添加数据
            stateMachine = new RocksStateMachine(raftPeers.getSelf());
            logStore.start();

            stateMachine.start();
        }
    }


    @Override
    public void stop() {
        logStore.stop();
//        stateMachine.stop();
        executorService.shutdown();
        START_STATE.compareAndSet(true, false);
    }

    public long getTerm() {
        return currentTerm.get();
    }

    public RaftPeers.PeerNode getVotedFor() {
        return votedFor;
    }

    public RaftNode setTerm(long newTerm) {
        this.currentTerm.set(newTerm);
        return this;
    }

    public RaftNode voteFor(RaftPeers.PeerNode votedForNode) {
        this.votedFor = votedForNode;
        return this;
    }


    /**
     * 将当前节点的状态设置为 nodeStatus
     */
    public RaftNode markStatus(NodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
        return this;
    }

    /**
     * 判断当前节点是否是leader节点
     */
    public boolean isLeader() {
        return nodeStatus == NodeStatus.LEADER;
    }


    /**
     * 将传入的节点设置为leader节点
     */
    public RaftNode setLeader(RaftPeers.PeerNode leader) {
        this.raftPeers.setLeader(leader);
        return this;
    }

    /**
     * 判断当前节点是否已经投票了
     */
    public boolean notVote() {
        return votedFor == null;
    }

    /**
     * 判断当前投票的节点是否是传入的候选节点
     *
     * @param candidateNode 候选节点
     */
    public boolean voteIsCandidate(RaftPeers.PeerNode candidateNode) {
        return votedFor.equals(candidateNode);
    }


    public RaftNode resetElectionTimeout() {
        long currentTimeout = electionTask.electionTimeout;
        //重置选举超时时间
        electionTask.resetElectionTimeout();
        log.info("[{}] reset election  timeout, time is: {}s", getSelfPeer(), currentTimeout / 1000);
        return this;
    }

    public RaftPeers.PeerNode getSelfPeer() {
        return raftPeers.getSelf();
    }


    private final AtomicBoolean changeTermLock = new AtomicBoolean();

    public boolean tryTermLock(){
        return changeTermLock.compareAndSet(false, true);
    }


    public boolean termUnlock(){
        return changeTermLock.compareAndSet(true, false);
    }


    /**
     * @param prevTerm 当前的term任期
     * @param newTerm  即将要改变的term任期
     */
    public boolean compareAndSwap(long prevTerm, long newTerm, RaftPeers.PeerNode votedFor) {
        //如果传入的任期与当前任期相同, 那么说明当前节点已经做过操作了，直接返回false
        if (newTerm == currentTerm.get()) {
            return false;
        }

        //否则的话通过CAS 保证只有一个节点可以操作成功
        // 1、当前节点变为候选节点时，需要自增term，并且将voteFor设置为自己
        // 2、候选节点调用 voteRpc请求当前节点时，如果请求节点的term > 当前节点，则将term设置为请求节点的term，voteFor设置为请求节点
        // 为了解决以上1、2冲突时的场景，保证在当前节点election超时变为候选节点之前，自己没有给其他节点投过票
        if (currentTerm.compareAndSet(prevTerm, newTerm)) {
            this.votedFor = votedFor;
            return true;
        }

        return false;
    }


    /**
     * 用于接受心跳
     */
    private class HeartBeakTask implements Runnable {

        /**
         * 心跳间隔基数, 默认为5000ms
         */
        public final long DEFAULT_HEART_BEAT_TICK = TimeUnit.SECONDS.toMillis(5L);
        public volatile long heartBeatTick = DEFAULT_HEART_BEAT_TICK;

        private final Logger log = LoggerFactory.getLogger(HeartBeakTask.class);

        public void setHeartBeatTick(long heartBeatTick) {
            this.heartBeatTick = heartBeatTick;
        }

        public long getHeartBeatTick() {
            return heartBeatTick;
        }

        public void resetHeartBeat() {
            setHeartBeatTick(DEFAULT_HEART_BEAT_TICK);
        }

        @Override
        public void run() {
            try {
                if (nodeStatus != NodeStatus.LEADER) {
                    return;
                }

                setHeartBeatTick(heartBeatTick - SCHEDULE_PERIOD_TIME);

                //如果心跳未超时则直接return
                if (heartBeatTick > 0) {
                    return;
                }

                resetHeartBeat();

                for (RaftPeers.PeerNode peerNode : raftPeers.getPeerNodes()) {
                    GrpcClient client = RequestClients.getClient(peerNode);
                    //发送心跳
                    HeartBeatRequest beatRequest = HeartBeatRequest.builder()
                            .term(currentTerm.get())
                            .leaderNode(raftPeers.getLeader()).build();

                    log.info("[{}] Send heart beat message: {}", getSelfPeer(), beatRequest);
                    HeartBeatResponse response = client.request(beatRequest, Duration.ofSeconds(5));

                    if (response.isSuccess()) {
                        //别人的任期大于自己，说明别人成了领导自己要跟随，这里针对网络分区的问题
                        if (response.getTerm() > currentTerm.get()) {
                            currentTerm.set(response.getTerm());
                            //todo 这里是不是没有必要设置voteFor = null 的情况
                            votedFor = null;

                            //如果当前节点的term < 请求节点的term，则可能出现网络分区的情况，将当前节点的状态设置为follower状态
                            nodeStatus = NodeStatus.FOLLOWER;
                        }
                    }
                }

            } catch (Exception error) {
                log.error("[{}] Send heartbeat error: ", getSelfPeer(), error);
            }
        }
    }

    /**
     * 用于触发超时选举
     */
    private class ElectionTask implements Runnable {

        private final Logger log = LoggerFactory.getLogger(ElectionTask.class);


        private volatile long electionTimeout;


        public ElectionTask() {
            //第一次的选举的超时时间，0 - 15s
            this.electionTimeout = ThreadLocalRandom.current().nextLong(0, DEFAULT_ELECTION_TIMEOUT);
        }


        public void setElectionTimeout(long electionTimeout) {
            this.electionTimeout = electionTimeout;
        }

        private long randomTime() {
            return ThreadLocalRandom.current().nextLong(5000);
        }

        public void resetElectionTimeout() {
            long timeout = DEFAULT_ELECTION_TIMEOUT + randomTime();
            setElectionTimeout(timeout);
        }

        @Override
        public void run() {

            if (nodeStatus == NodeStatus.LEADER) {
                return;
            }

            long currentTime = System.currentTimeMillis();

            //每次减去500ms
            setElectionTimeout(electionTimeout - SCHEDULE_PERIOD_TIME);


            //如果大于0表示还没有到达选举时间
            if (electionTimeout > 0) {
                return;
            }

            log.info("[{}] Election task start", raftPeers.getSelf());

            log.info("[{}] current electionTime is: {}", getSelfPeer(), electionTimeout);
            log.info("[{}] Trigger election current time is: {}", getSelfPeer(), currentTime / 1000);
            log.info("[{}] election lastElection is:{}, currentTime is: {}",
                    getSelfPeer(),
                    electionTimeout / 1000,
                    currentTime / 1000);


            //重置选举时间
            resetElectionTimeout();

            //将自己变为候选节点
            nodeStatus = NodeStatus.CANDIDATE;
            log.info("[{}] The current node {} can be CANDIDATE node", raftPeers.getSelf(), raftPeers.getSelf());


            long term = currentTerm.get();

            //todo:
            //将当前节点的任期 + 1
            currentTerm.incrementAndGet();
            //将票投给自己
            votedFor = raftPeers.getSelf();


            //获取其他raft节点
            Set<RaftPeers.PeerNode> peerNodes = raftPeers.getPeerNodes();

            //成功数量默认为1，因为当前节点会把票投给自己
            int successCount = 1;

            Iterator<RaftPeers.PeerNode> iterator = peerNodes.iterator();

            while (iterator.hasNext()) {

                RaftPeers.PeerNode peerNode = iterator.next();
                GrpcClient grpcClient = RequestClients.getClient(peerNode);
                VoteRequest voteRequest = new VoteRequest();
                //将投票的节点设置为自己
                voteRequest.setCandidateNode(raftPeers.getSelf());
                //获取最后一条日志的index
                voteRequest.setLastLogIndex(logStore.getLastIndex());
                //获取最后一条日志的term
                voteRequest.setLastLogTerm(logStore.getLastTerm());
                //将请求地址设置为当前节点地址
                voteRequest.setRequestAddress(raftPeers.getSelfAddress());
                //将当前任期编号放入到请求中
                voteRequest.setTerm(currentTerm.get());
                log.info("[{}] Call rpc to remote address: {}", getSelfPeer(), peerNode);
                try {
                    //发起投票请求
                    ResponseCommand responseCommand = grpcClient.request(voteRequest, Duration.ofSeconds(8));
                    log.info("[{}] Vote response result is: {}, vote node is: {}", getSelfPeer(), responseCommand, peerNode);

                    //这里要判断下response是否成功，如果不成功可能是由于server端引起的，可能返回的并不是对应的ResponseCommand
                    if (responseCommand.isSuccess()) {
                        VoteResponse response = (VoteResponse) responseCommand;
                        //如果把票投给自己将successCount + 1
                        if (response.isVoteGranted()) {
                            successCount++;
                        } else {
                            //如果没有投给自己,则判断返回的term是否大于当前term,如果大于当前term，则将当前term的值设置为其他node节点的term
                            long nodeTerm = response.getTerm();
                            if (nodeTerm >= currentTerm.get()) {
                                currentTerm.set(nodeTerm);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("[{}] Leader vote rpc error, cause is: ", getSelfPeer(), e);
                    //当节点请求发生异常时，删除异常节点，方便用于统计
                    iterator.remove();
                }
            }


            //如果当前节点已经变为follower节点，则进入下一次定时任务
            if (nodeStatus == NodeStatus.FOLLOWER) {
                return;
            }

            log.info("[{}] Election successCount is {}: ", getSelfPeer(), successCount);

            //判断投票成功的数量是否大于1/2
            if (successCount > raftPeers.nodeCount() / 2) {
                //如果大于二分之一，则将当前节点设置为leader节点，并且重置其他节点的时间
                nodeStatus = NodeStatus.LEADER;
                raftPeers.markSelfLeader();
                log.info("[{}] Leader peer is :{}", getSelfPeer(), raftPeers.getLeader());

                //立即向其他节点发送心跳
                executorService.execute(heartBeakTask);
                //TODO 当自己成为leader时,要同步日志

                initIndex();
            }

            //将voteFor设置为null，在失败的情况下可以保证其他节点的正常选举
            votedFor = null;

            log.info("[{}] Schedule next time for election", getSelfPeer());

        }

    }

    /* ============ 所有服务器上经常变的 ============= */

    /**
     * 已知被提交的最大日志条目索引
     */
    @Getter
    @Setter
    volatile long commitIndex;

    /**
     * 已被状态机执行的最大日志条目索引
     */
    @Setter
    @Getter
    volatile long lastApplied = 0;

    /* ========== 在领导人里经常改变的(选举后重新初始化) ================== */

    /**
     * 对于每一个follower, 记录需要发给他的下一条日志条目的索引
     */
    @Getter
    Map<RaftPeers.PeerNode, Long> nextIndexes;

    /**
     * 对于每一个follower, 记录已经复制完成的最大日志条目索引
     */
    @Getter
    Map<RaftPeers.PeerNode, Long> matchIndexes;

    /**
     * 初始化所有的 nextIndex 值为自己的最后一条日志的 index + 1. 如果下次 RPC 时, 跟随者和leader 不一致,就会失败.
     * 那么 leader 尝试递减 nextIndex 并进行重试.最终将达成一致.
     */
    private void initIndex() {
        nextIndexes = new ConcurrentHashMap<>();
        matchIndexes = new ConcurrentHashMap<>();
        for (RaftPeers.PeerNode peer : raftPeers.getPeerNodes()) {
            nextIndexes.put(peer, logStore.getLastIndex() + 1);
            matchIndexes.put(peer, 0L);
        }
    }

    public long getNextIndex(RaftPeers.PeerNode peerNode) {
        return nextIndexes.getOrDefault(peerNode, 0L);
    }

}
