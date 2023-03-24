package com.dc.raft.jraft;

import com.alipay.sofa.jraft.*;
import com.alipay.sofa.jraft.closure.TaskClosure;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.core.NodeImpl;
import com.alipay.sofa.jraft.entity.LeaderChangeContext;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.RaftOutter;
import com.alipay.sofa.jraft.entity.Task;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.RpcOptions;
import com.alipay.sofa.jraft.rpc.*;
import com.alipay.sofa.jraft.rpc.impl.BoltRpcClient;
import com.alipay.sofa.jraft.rpc.impl.ConnectionClosedEventListener;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.alipay.sofa.jraft.storage.impl.LogManagerImpl;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import com.alipay.sofa.jraft.util.Endpoint;
import com.dc.raft.store.log.LogStore;
import io.netty.buffer.ByteBuf;
import jodd.util.Bits;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class JraftClient {

    public static void main(String[] args) {
        Endpoint endpoint = new Endpoint("localhost",8080);
        String s = endpoint.toString();

        PeerId peerId = PeerId.parsePeer("localhost:8081");
        PeerId peerId1 = PeerId.parsePeer("localhost:8082");
        PeerId peerId2 = PeerId.parsePeer("localhost:8083");

        boolean parse = peerId.parse(s);

        Configuration configuration = new Configuration();

        configuration.addPeer(peerId);
        configuration.addPeer(peerId1);
        configuration.addPeer(peerId2);


        Closure closure = new Closure() {
            @Override
            public void run(Status status) {
                System.out.println(status);
            }
        };


        Task task = new Task();
        task.setData(ByteBuffer.wrap("Hello World".getBytes(StandardCharsets.UTF_8)));
        task.setExpectedTerm(-1);
        task.setDone(new TaskClosure() {
            @Override
            public void onCommitted() {
                System.out.println("日志提交");
            }

            @Override
            public void run(Status status) {
                System.out.println(status.toString());
            }
        });

        Node raftNode = RaftServiceFactory.createRaftNode("", peerId);


        NodeImpl node = (NodeImpl) raftNode;
        RaftClientService rpcService = node.getRpcService();


        com.alipay.remoting.rpc.RpcClient rpcClient = new com.alipay.remoting.rpc.RpcClient();

        RpcClient raftRpcClient = new BoltRpcClient(rpcClient);


        RaftGroupService raftGroupService = new RaftGroupService("", peerId, new NodeOptions());


        Node start = raftGroupService.start(true);

        start.apply(new Task());


        NodeOptions nodeOptions = new NodeOptions();

        nodeOptions.setLogUri("");

        NodeManager.getInstance().add(raftNode);

        node.snapshot(new Closure() {
            @Override
            public void run(Status status) {

            }
        });

        //应用状态机
        StateMachine stateMachine = new StateMachine() {
            @Override
            public void onApply(Iterator iter) {

            }

            @Override
            public void onShutdown() {

            }

            @Override
            public void onSnapshotSave(SnapshotWriter writer, Closure done) {
                writer.addFile("");

                done.run(new Status());
            }

            @Override
            public boolean onSnapshotLoad(SnapshotReader reader) {
                RaftOutter.SnapshotMeta load = reader.load();

                return false;
            }

            @Override
            public void onLeaderStart(long term) {

            }

            @Override
            public void onLeaderStop(Status status) {

            }

            @Override
            public void onError(RaftException e) {

            }

            @Override
            public void onConfigurationCommitted(Configuration conf) {

            }

            @Override
            public void onStopFollowing(LeaderChangeContext ctx) {

            }

            @Override
            public void onStartFollowing(LeaderChangeContext ctx) {

            }
        };


        RpcServer raftRpcServer = RaftRpcServerFactory.createRaftRpcServer(peerId.getEndpoint());


        raftRpcServer.registerConnectionClosedEventListener(new ConnectionClosedEventListener() {
            @Override
            public void onClosed(String remoteAddress, Connection conn) {

            }
        });

        raftRpcServer.init(null);

        RaftRpcServerFactory.addRaftRequestProcessors(raftRpcServer);


        RouteTable instance = RouteTable.getInstance();

        instance.updateConfiguration("", new Configuration());

        CliClientService clientService = new CliClientServiceImpl();

        clientService.init(new CliOptions());

    }

    @Test
    public void test(){
        int i = 1 << 31;
        System.out.println(i);
    }
}
