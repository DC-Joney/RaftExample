package com.dc.raft.jraft;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.RaftGroupService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.core.TimerManager;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.*;
import com.alipay.sofa.jraft.rpc.impl.ConnectionClosedEventListener;
import com.alipay.sofa.jraft.util.Endpoint;
import com.dc.raft.jraft.dto.IncrementAndGetRequest;
import com.dc.raft.jraft.statemachine.CounterStateMachine;
import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CounterServer {

    private RaftGroupService raftGroupService;
    private Node node;
    private CounterStateMachine stateMachine;


    public CounterStateMachine getStateMachine() {
        return stateMachine;
    }

    /**
     *
     * @param dataPath 数据存储的base目录
     * @param groupId raft group id
     * @param serverId node id
     * @param nodeOptions 节点配置
     */
    public CounterServer(String dataPath, String groupId, PeerId serverId,
                         NodeOptions nodeOptions){


        Path basePath = Paths.get(dataPath);


        TimerManager timerManager = new TimerManager(4);

        RpcServer rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
        RaftRpcServerFactory.addRaftRequestProcessors(rpcServer);

        rpcServer.registerConnectionClosedEventListener(new ConnectionClosedEventListener() {
            @Override
            public void onClosed(String remoteAddress, Connection conn) {
                System.out.println(remoteAddress);
                System.out.println(conn);
            }
        });



        rpcServer.registerProcessor(new RpcProcessor<Object>() {
            @Override
            public void handleRequest(RpcContext rpcCtx, Object request) {
                if (node.isLeader()) {
                    rpcCtx.sendResponse();
                }

                node.apply();
            }

            @Override
            public String interest() {
                return IncrementAndGetRequest.class.getName();
            }
        });



        //创建目录



    }


}
