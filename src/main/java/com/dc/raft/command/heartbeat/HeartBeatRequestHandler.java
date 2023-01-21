package com.dc.raft.command.heartbeat;

import com.dc.raft.annotation.RpcHandler;
import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.handler.RequestHandler;
import com.dc.raft.node.NodeStatus;
import com.dc.raft.node.RaftNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RpcHandler
public class HeartBeatRequestHandler implements RequestHandler {

    private final RaftNode raftNode;

    public HeartBeatRequestHandler(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    @Override
    public ResponseCommand handle(RequestCommand requestCommand) {
        log.info("[{}] Receive heartbeat message: {}",raftNode.getSelfPeer(), requestCommand);
        HeartBeatRequest request = (HeartBeatRequest) requestCommand;

        //如果发送心跳节点的term < 当前节点的term，则认为该节点是有问题的，直接返回
        if (request.getTerm() < raftNode.getTerm()) {
            return HeartBeatResponse.fail(raftNode.getTerm());
        }

        log.info("[{}] reset current info", raftNode.getSelfPeer());
        //重置当前节点的心跳，并且将leader节点设置为发送的节点
        raftNode.setLeader(request.getLeaderNode()).resetElectionTimeout();
        //将当前节点的状态设置为follower
        raftNode.markStatus(NodeStatus.FOLLOWER);
        //将当前节点的term设置为leader节点的term
        raftNode.setTerm(request.getTerm());
        //将voteFor设置为null, 保证下一次选举时可以帮助其他节点进行选举
        raftNode.voteFor(null);

        return HeartBeatResponse.success(raftNode.getTerm());
    }

    @Override
    public Class<?> supportType() {
        return HeartBeatRequest.class;
    }
}
