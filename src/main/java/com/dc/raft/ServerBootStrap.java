package com.dc.raft;

import com.dc.raft.handler.RequestHandler;
import com.dc.raft.handler.RequestHandlerAcceptor;
import com.dc.raft.node.RaftPeers;
import com.dc.raft.rpc.GrpcServer;
import com.google.common.collect.Sets;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * 服务端启动
 */
public class ServerBootStrap extends RemoteBootStrap {

    private GrpcServer grpcServer;


    public ServerBootStrap(RaftPeers.PeerNode self, Set<RaftPeers.PeerNode> others) {
        super(self, others);
    }

    @Override
    protected void initClient(Set<RequestHandler> requestHandlers) {
        this.grpcServer = new GrpcServer(raftPeers.getSelf().getAddress().getPort());
        RequestHandlerAcceptor handlerAcceptor = new RequestHandlerAcceptor(requestHandlers);
        grpcServer.setHandlerAcceptor(handlerAcceptor);
        this.grpcServer.start();
    }

    @Override
    public void stop() {
        grpcServer.stop();
    }

    @Override
    protected Set<String> scanPackageHandlers() {
        return Sets.newHashSet("com.dc.raft.vote","com.dc.raft.heartbeat");
    }


    public static void main(String[] args) throws Exception {
        Set<RaftPeers.PeerNode> peerNodes = Sets.newHashSet();
        for (int port = 8080; port < 8083; port++) {
            peerNodes.add(RaftPeers.PeerNode.create(new InetSocketAddress("127.0.0.1", port)));
        }

        for (RaftPeers.PeerNode peerNode : peerNodes) {
            new ServerBootStrap(peerNode, peerNodes).start();
        }
    }
}
