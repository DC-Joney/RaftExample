package com.dc.raft.command.vote;

import com.dc.raft.annotation.RpcHandler;
import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.handler.RequestHandler;
import com.dc.raft.store.LogEntry;
import com.dc.raft.store.log.LogStore;
import com.dc.raft.network.Metadta;
import com.dc.raft.node.NodeStatus;
import com.dc.raft.node.RaftNode;
import com.dc.raft.node.RaftPeers;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用于处理投票请求
 */
@Slf4j
@RpcHandler
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoteRequestHandler implements RequestHandler {

    final AtomicBoolean voteLock = new AtomicBoolean(false);

    private final RaftNode raftNode;

    private final LogStore logStore;

    public VoteRequestHandler(RaftNode raftNode){
        this.raftNode = raftNode;
        this.logStore = raftNode.getLogStore();
    }

    @Override
    public  ResponseCommand handle(RequestCommand request) {
        try {
            VoteRequest voteRequest = (VoteRequest) request;
            RaftPeers.PeerNode candidateNode = voteRequest.getCandidateNode();
            log.info("[{}] Receive vote request: {}", raftNode.getSelfPeer(), candidateNode);

            //已经给别人投过票了
            if (!voteLock.compareAndSet(false, true)) {
                return VoteResponse.fail(raftNode.getTerm());
            }

            //当前节点的term
            long term = raftNode.getTerm();

            //如果请求投票节点的term < 当前节点的term，则返回当前节点的term
            if (voteRequest.getTerm() < term) {
                return VoteResponse.fail(term);
            }

            //如果候选节点的term > 当前节点的term，并且投过票的节点是请求节点的话
            //1、如果当前节点还没有发起投票选举或者已发起投票选举时，判断请求节点的term是否大于当前节点，如果大于当前节点则认为对象是合法的，那么就会将自己变为follower状态
            //2、
            if (voteRequest.getTerm() > term || raftNode.voteIsCandidate(candidateNode)) {

                //获取当前节点的最后一条日志
                LogEntry lastLog = raftNode.getLogStore().getLastLog();
                if (lastLog != null) {
                    //如果最后一条日志的索引
                    Long lastIndex = lastLog.getIndex();
                    //获取最后一条日志的任期
                    long lastTerm = lastLog.getTerm();
                    //如果最后一条日志的索引 > 请求节点的最后一条日志索引 或者是最后一条日志对应term > 请求节点的term，则返回失败
                    if (lastIndex > voteRequest.getLastLogIndex() || lastTerm > voteRequest.getTerm()) {
                        return VoteResponse.fail();
                    }
                }

                raftNode.resetElectionTimeout();

                //将当前节点设置为follower节点，并且将term设置为请求节点的term，并且将voteFor设置为请求的候选节点
                raftNode.markStatus(NodeStatus.FOLLOWER)
                        .setTerm(voteRequest.getTerm())
                        .voteFor(voteRequest.getCandidateNode());

                log.info("[{}] Vote for candidate node: {}", raftNode.getSelfPeer(), candidateNode);

                //返回当前节点的任期
                return VoteResponse.ok(raftNode.getTerm());


            }
            return VoteResponse.fail(raftNode.getTerm());
        } finally {
            voteLock.set(false);
        }
    }


    @Override
    public Class<?> supportType() {
        return VoteRequest.class;
    }
}
