package com.dc.raft.heartbeat;

import com.dc.raft.annotation.RpcHandler;
import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.handler.RequestHandler;
import com.dc.raft.network.Metadta;
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

        log.info("Receive heartbeat message: {}", requestCommand);
        HeartBeatRequest request = (HeartBeatRequest) requestCommand;

        //如果发送心跳节点的term < 当前节点的term，则认为该节点是有问题的，直接返回
        if (request.getTerm() < raftNode.getTerm()) {
            return HeartBeatResponse.fail(raftNode.getTerm());
        }

        raftNode.setLeader(request.getLeaderNode())
                .resetLastHeartBeatTime()
                .resetLastElectionTime();

        if (request.getTerm() >= raftNode.getTerm()) {
            raftNode.markStatus(NodeStatus.FOLLOWER);

            //将当前节点的term设置为leader节点的term
            raftNode.setTerm(request.getTerm());
        }

        return HeartBeatResponse.success(raftNode.getTerm());
    }

    @Override
    public boolean support(Metadta metadta) {
        return HeartBeatRequest.class.getSimpleName().equals(metadta.getType());
    }
}
