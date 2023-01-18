package com.dc.raft.node;

import com.dc.raft.LifeCycle;
import com.dc.raft.RequestClients;
import com.dc.raft.vote.VoteRequest;
import com.dc.raft.vote.VoteResponse;
import com.dc.raft.rpc.GrpcClient;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Set;
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


    @Override
    public void start() {
        if (START_STATE.compareAndSet(false, true)) {
            executorService.schedule(electionTask, electionTask.getElectionTime(), TimeUnit.SECONDS);
            executorService.schedule(heartBeakTask, heartBeakTask.getHeartBeatTick(), TimeUnit.SECONDS);
        }
    }


    @Override
    public void stop() {

    }

    public long getTerm(){
        return currentTerm.get();
    }

    public RaftPeers.PeerNode getVotedFor() {
        return votedFor;
    }

    public RaftNode setTerm(long newTerm){
        this.currentTerm.set(newTerm);
        return this;
    }

    public RaftNode voteFor(RaftPeers.PeerNode votedForNode){
        this.votedFor = votedForNode;
        return this;
    }


    /**
     * 将当前节点的状态设置为 nodeStatus
     */
    public RaftNode markStatus(NodeStatus nodeStatus){
        this.nodeStatus = nodeStatus;
        return this;
    }

    /**
     * 将传入的节点设置为leader节点
     */
    public RaftNode setLeader(RaftPeers.PeerNode leader){
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
     * @param candidateNode 候选节点
     */
    public boolean voteIsCandidate(RaftPeers.PeerNode candidateNode) {
        return votedFor.equals(candidateNode);
    }


    /**
     * 用于接受心跳
     */
    private static class HeartBeakTask implements Runnable {


        /**
         * 心跳间隔基数, 单位为：s
         */
        public final long heartBeatTick = 5;


        public long getHeartBeatTick() {
            return heartBeatTick;
        }

        @Override
        public void run() {

        }
    }

    /**
     * 用于触发超时选举
     */
    private class ElectionTask implements Runnable {

        /**
         * 单位为 s
         */
        public volatile AtomicLong election = new AtomicLong();


        public long getElectionTime() {
            return election.get();
        }

        @Override
        public void run() {
            if (nodeStatus == NodeStatus.LEADER) {
                scheduleNextTime();
            }

            long currentTime = System.currentTimeMillis();

            long electionTime = election.addAndGet(ThreadLocalRandom.current().nextInt(50));

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

            int successCount = 0;

            for (RaftPeers.PeerNode peerNode : peerNodes) {
                GrpcClient grpcClient = RequestClients.getClient(peerNode);
                VoteRequest voteRequest = new VoteRequest();
                //将投票的节点设置为自己
                voteRequest.setCandidateNode(raftPeers.getSelf());
                //将请求地址设置为当前节点地址
                voteRequest.setRequestAddress(raftPeers.getSelfAddress());
                //将当前任期编号放入到请求中
                voteRequest.setTerm(currentTerm.get());
                VoteResponse response = grpcClient.request(voteRequest, Duration.ofSeconds(3));
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
            }


            if (successCount > raftPeers.nodeCount() / 2) {
                nodeStatus = NodeStatus.LEADER;
                raftPeers.markSelfLeader();
                //TODO 当自己成为leader时,要同步日志
            }

            //
            votedFor = null;

            //触发下一次定时任务
            scheduleNextTime();
        }


        private void scheduleNextTime() {
            executorService.schedule(this, getElectionTime(), TimeUnit.SECONDS);
        }
    }

}
