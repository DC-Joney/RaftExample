package com.dc.raft;

import com.dc.raft.handler.RequestHandler;
import com.dc.raft.handler.RequestHandlerAcceptor;
import com.dc.raft.node.RaftPeers;
import com.dc.raft.rpc.GrpcServer;
import com.google.common.collect.Sets;
import org.springframework.boot.ApplicationRunner;

import java.util.Set;

/**
 * 服务端启动
 */
public class ServerBootStrap extends RemoteBootStrap {

    private GrpcServer grpcServer;

    private final int port;

    public ServerBootStrap(RaftPeers.PeerNode self, Set<RaftPeers.PeerNode> others) {
        super(self, others);
        this.port = self.getAddress().getPort();
    }

    @Override
    protected void initClient(Set<RequestHandler> requestHandlers) {
        this.grpcServer = new GrpcServer(port);
        RequestHandlerAcceptor handlerAcceptor = new RequestHandlerAcceptor(requestHandlers);
        grpcServer.setHandlerAcceptor(handlerAcceptor);
        this.grpcServer.start();
    }

    @Override
    public void stop() {
        super.stop();
        grpcServer.stop();
    }

    @Override
    protected Set<String> scanPackageHandlers() {
        return Sets.newHashSet("com.dc.raft.command.vote", "com.dc.raft.command.heartbeat");
    }


}
