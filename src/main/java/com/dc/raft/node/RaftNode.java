package com.dc.raft.node;

import com.dc.raft.LifeCycle;
import com.dc.raft.RequestClients;
import com.dc.raft.heartbeat.HeartBeatRequest;
import com.dc.raft.heartbeat.HeartBeatResponse;
import com.dc.raft.vote.VoteRequest;
import com.dc.raft.vote.VoteResponse;
import com.dc.raft.rpc.GrpcClient;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RaftNode implements LifeCycle {

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
    private RaftPeers raftPeers;

    /**
     * 上一次选举的时间
     */
    private volatile long lastElectionTime = 0L;

    /**
     * 上次一心跳时间戳
     */
    private volatile long lastHeartBeatTime = 0;

    /**
     * 当前节点的状态
     */
    private NodeStatus nodeStatus = NodeStatus.FOLLOWER;


    /* ============ 所有服务器上持久存在的 ============= */

    /**
     * 服务器最后一次知道的任期号（初始化为 0，持续递增）
     */
    private final AtomicLong currentTerm = new AtomicLong();

    /**
     * 在当前获得选票的候选人的 Id
     */
    volatile RaftPeers.PeerNode votedFor;


    public RaftNode() {
        this.executorService = Executors.newScheduledThreadPool(2);
    }

    @Override
    public void start() {
        if (START_STATE.compareAndSet(false, true)) {
            executorService.schedule(electionTask, electionTask.getElectionTime(), TimeUnit.MILLISECONDS);
            executorService.scheduleWithFixedDelay(heartBeakTask,heartBeakTask.getHeartBeatTick(), heartBeakTask.getHeartBeatTick(), TimeUnit.MILLISECONDS);
        }
    }


    @Override
    public void stop() {
        executorService.shutdown();
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


    public RaftNode resetLastHeartBeatTime() {
        this.lastHeartBeatTime = System.currentTimeMillis();
        return this;
    }

    public RaftNode resetLastElectionTime() {
        this.lastElectionTime = System.currentTimeMillis();
        return this;
    }



    /**
     * 用于接受心跳
     */
    private class HeartBeakTask implements Runnable {


        /**
         * 心跳间隔基数, 单位为：s
         */
        public final long heartBeatTick = 5 * 1000;


        public long getHeartBeatTick() {
            return heartBeatTick;
        }

        @Override
        public void run() {
            try {
                if (nodeStatus != NodeStatus.LEADER) {
                    return;
                }

                long currentTime = System.currentTimeMillis();

                if (currentTime - lastHeartBeatTime < heartBeatTick)
                    return;

                for (RaftPeers.PeerNode peerNode : raftPeers.getPeerNodes()) {
                    GrpcClient client = RequestClients.getClient(peerNode);
                    HeartBeatRequest beatRequest = HeartBeatRequest.builder()
                            .term(currentTerm.get())
                            .leaderNode(raftPeers.getLeader())
                            .build();

                    log.info("Send heart beat message: {}", beatRequest);
                    HeartBeatResponse response = client.request(beatRequest, Duration.ofSeconds(5));

                    log.info("result response is: {}", response);
                    if (response.isSuccess()) {
                        //别人的任期大于自己，说明别人成了领导自己要跟随
                        if (response.getTerm() > currentTerm.get()) {
                            currentTerm.compareAndSet(currentTerm.get(), response.getTerm());
                            votedFor = null;
                            nodeStatus = NodeStatus.FOLLOWER;
                        }
                    }
                }

                lastHeartBeatTime = currentTime;
            } catch (Exception error) {
                log.error("send heartbeat error: ", error);
            }
        }
    }

    /**
     * 用于触发超时选举
     */
    private class ElectionTask implements Runnable {

        /**
         * 单位为 s
         */
        public volatile AtomicLong election = new AtomicLong(15 * 1000);


        public long getElectionTime() {
            return election.get();
        }

        @Override
        public void run() {
            if (nodeStatus == NodeStatus.LEADER) {
                scheduleNextTime();
            }

            long currentTime = System.currentTimeMillis();

            long electionTime = election.addAndGet(ThreadLocalRandom.current().nextInt(100));
            log.info("current electionTime is: {}", electionTime);

            if (lastElectionTime + electionTime < currentTime) {
                scheduleNextTime();
            }

            //将自己变为候选节点
            nodeStatus = NodeStatus.CANDIDATE;
            log.info("The current node {} can be CANDIDATE node", raftPeers.getSelf());

            //将当前节点的任期 + 1
            currentTerm.incrementAndGet();

            //将票投给自己
            votedFor = raftPeers.getSelf();

            Set<RaftPeers.PeerNode> peerNodes = raftPeers.getPeerNodes();

            int successCount = 1;

            for (RaftPeers.PeerNode peerNode : peerNodes) {
                GrpcClient grpcClient = RequestClients.getClient(peerNode);
                VoteRequest voteRequest = new VoteRequest();
                //将投票的节点设置为自己
                voteRequest.setCandidateNode(raftPeers.getSelf());
                //将请求地址设置为当前节点地址
                voteRequest.setRequestAddress(raftPeers.getSelfAddress());
                //将当前任期编号放入到请求中
                voteRequest.setTerm(currentTerm.get());
                log.info("Call rpc to remote address: {}", peerNode);
                try {
                    VoteResponse response = grpcClient.request(voteRequest, Duration.ofSeconds(8));
                    log.info("Vote response result is: {}", response);
                    if (response.isSuccess()) {
                        if (response.isVoteGranted()) {
                            successCount++;
                        } else {
                            long nodeTerm = response.getTerm();
                            if (nodeTerm >= currentTime) {
                                currentTime = nodeTerm;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("execute error, cause is: ", e);
                }
            }


            log.info("successCount is {}: ", successCount);
            if (successCount > raftPeers.nodeCount() / 2) {
                nodeStatus = NodeStatus.LEADER;
                raftPeers.markSelfLeader();
                log.info("Leader peer is :{}", raftPeers.getLeader());
                //TODO 当自己成为leader时,要同步日志
            }

            //
            votedFor = null;

            log.info("evicate next time");
            //触发下一次定时任务
            scheduleNextTime();
        }


        private void scheduleNextTime() {
            executorService.schedule(this, getElectionTime(), TimeUnit.SECONDS);
        }
    }

}
