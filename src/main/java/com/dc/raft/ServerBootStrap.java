package com.dc.raft;

import com.dc.raft.rpc.GrpcServer;

/**
 * 服务端启动
 */
public class ServerBootStrap extends RemoteBootStrap{

    private GrpcServer grpcServer;

    @Override
    public void start() throws Exception {
        super.start();
        this.grpcServer = new GrpcServer(8080);
        grpcServer.setHandlerAcceptor(handlerAcceptor);
    }

    @Override
    public void stop() {
        grpcServer.stop();
    }
}
