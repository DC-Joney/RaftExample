package com.dc.raft.command.client;

import com.dc.raft.RequestClients;
import com.dc.raft.annotation.RpcHandler;
import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.command.replication.ReplicationRequest;
import com.dc.raft.command.replication.ReplicationResponse;
import com.dc.raft.handler.RequestHandler;
import com.dc.raft.node.NodeStatus;
import com.dc.raft.node.RaftNode;
import com.dc.raft.node.RaftPeers;
import com.dc.raft.rpc.GrpcClient;
import com.dc.raft.store.LogEntry;
import com.dc.raft.store.log.LogStore;
import com.dc.raft.store.statemachine.DataStateMachine;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@RpcHandler
public class ClientRequestHandler implements RequestHandler {

    private final RaftNode raftNode;

    private final DataStateMachine stateMachine;

    private final LogStore logStore;

    public ClientRequestHandler(RaftNode raftNode) {
        this.raftNode = raftNode;
        this.stateMachine = raftNode.getStateMachine();
        this.logStore = raftNode.getLogStore();
    }

    @Override
    public ResponseCommand handle(RequestCommand requestCommand) {
        ClientRequest request = (ClientRequest) requestCommand;
        //如果当前节点不是leader节点，则将请求转发到leader
        if (!raftNode.isLeader()) {
            //获取leader节点
            RaftPeers.PeerNode leaderNode = raftNode.getRaftPeers().getLeader();
            //将请求转发到leader节点
            return RequestClients.getClient(leaderNode).request(request, Duration.ofSeconds(5));
        }

        //如果是get请求则返回状态集中的保存的数据，否则是put请求
        if (request.typeWith(ClientRequest.ClientType.GET)) {
            return handleGet(request);
        } else if (request.typeWith(ClientRequest.ClientType.DELETE)) {
            return handleDelete(request);
        } else {
            return handleSave(request);
        }

    }


    private ResponseCommand handleGet(ClientRequest request) {
        LogEntry logEntry = stateMachine.getLog(request.getKey());
        if (logEntry != null) {
            return ClientResponse.create(logEntry.getKey(), logEntry.getValue());
        }

        return ClientResponse.fail("Cannot find value of key: " + request.getKey());
    }

    private ResponseCommand handleDelete(ClientRequest request) {
        if (stateMachine.delete(request.getKey())) {
            return ClientResponse.ok();
        }

        return ClientResponse.fail("Delete key error: " + request.getKey());
    }


    private ResponseCommand handleSave(ClientRequest request) {

        //创建日志信息
        LogEntry logEntry = LogEntry.newBuilder()
                .key(request.getKey())
                .value(request.getValue())
                .term(raftNode.getTerm())
                .build();

        //预提交到本地日志
        logStore.write(logEntry);

        //写入成功的数量
        int successCount = 0;
        for (RaftPeers.PeerNode peerNode : raftNode.getRaftPeers()) {
            if (replication(peerNode, logEntry))
                successCount++;
        }

        if (successCount >= (raftNode.getRaftPeers().nodeCount() / 2)) {
            stateMachine.apply(logEntry);
            return ClientResponse.ok();
        }

        //将数据删除
        logStore.truncate(logEntry.getIndex());
        return ClientResponse.fail("cannot replication node > 1/2");

    }

    /**
     * 数据复制
     *
     * @param peerNode 被复制的节点
     */
    private boolean replication(RaftPeers.PeerNode peerNode, LogEntry logEntry) {

        while (true) {

            ReplicationRequest request = new ReplicationRequest();

            request.setTerm(raftNode.getTerm())
                    .setLeader(raftNode.getRaftPeers().getLeader())
                    .setLeaderCommit(raftNode.getCommitIndex());

            //获取需要复制节点的下一条nextIndex
            long nextIndex = raftNode.getNextIndex(peerNode);

            LinkedList<LogEntry> logEntries = new LinkedList<>();

            //如果新发送的条目所以大于 上一次发送的所有 ，则将全部大于上传所以的条目都进行添加
            //否则只添加当前条目
            if (logEntry.getIndex() > nextIndex) {
                List<LogEntry> entries = raftNode.getLogStore().readRange(nextIndex, logEntry.getIndex());
                logEntries.addAll(entries);
            } else {
                logEntries.add(logEntry);
            }

            //获取日志索引列表中的日志的前一个日志索引
            LogEntry prevLog = raftNode.getLogStore().getPrevLog(logEntries.getFirst().getIndex());

            request.setLogEntries(logEntries);

            //如果前一个日志索引不为空，则设置prevIndex 以及 prevTerm
            if (prevLog != null) {
                request.setPrevIndex(prevLog.getIndex())
                        .setPrevTerm(prevLog.getTerm());
            }

            //发送请求
            GrpcClient grpcClient = RequestClients.getClient(peerNode);
            ResponseCommand responseCommand = grpcClient.request(request);
            if (responseCommand.isSuccess()) {
                ReplicationResponse response = (ReplicationResponse) responseCommand;
                //如果复制成功 更新nextIndexes，更新matchIndexes
                if (response.isSuccess()) {
                    // 对于每一个follower, 记录需要发给他的下一条日志条目的索引
                    raftNode.getNextIndexes().put(peerNode, logEntry.getIndex() + 1);
                    // 对于每一个follower, 记录已经复制完成的最大日志条目索引
                    raftNode.getMatchIndexes().put(peerNode, logEntry.getIndex() + 1);
                    return true;
                }

                //如果被复制节点的term > 当前节点的term, 则改变当前节点的节点状态
                if (response.getTerm() > raftNode.getTerm()) {
                    raftNode.setTerm(request.getTerm())
                            .markStatus(NodeStatus.FOLLOWER);

                    return false;
                }


                //否则的话，将nextIndex - 1，继续重试直到成功为止
                //这里不需要判断nextIndex 是否 == 0, 因为当index == 0时，获取的prevIndex 已经是 -1了
                raftNode.getNextIndexes().put(peerNode, nextIndex - 1);

            }
        }

    }

    @Override
    public Class<?> supportType() {
        return ClientRequest.class;
    }
}
