package com.dc.raft.vote;

import com.dc.raft.ResultHandler;
import com.dc.raft.annotation.RpcHandler;
import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.handler.RequestHandler;
import com.dc.raft.network.Metadta;
import com.dc.raft.node.NodeStatus;
import com.dc.raft.node.RaftNode;
import com.dc.raft.node.RaftPeers;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用于处理投票请求
 */
@RpcHandler
public class VoteRequestHandler implements RequestHandler {

    /**
     * 当前 Raft节点
     */
    RaftNode raftNode;

    AtomicBoolean voteLock = new AtomicBoolean(false);

    @Override
    public  ResponseCommand handle(RequestCommand request) {
        try {
            VoteRequest voteRequest = (VoteRequest) request;
            RaftPeers.PeerNode candidateNode = voteRequest.getCandidateNode();

            if (!voteLock.compareAndSet(false, true)) {
                return VoteResponse.fail(raftNode.getTerm());
            }

            if (voteRequest.getTerm() < raftNode.getTerm()) {
                return VoteResponse.fail(raftNode.getTerm());
            }

            if (raftNode.notVote() || raftNode.voteIsCandidate(candidateNode)) {
                raftNode.markStatus(NodeStatus.FOLLOWER)
                        .setTerm(voteRequest.getTerm())
                        .voteFor(voteRequest.getCandidateNode());

                return VoteResponse.ok(raftNode.getTerm());
            }

            return VoteResponse.fail(raftNode.getTerm());
        } finally {
            voteLock.set(false);
        }
    }

    @Override
    public boolean support(Metadta metadta) {
        return metadta.getType().equals(VoteRequest.class.getSimpleName());
    }
}
