package com.dc.raft;

import com.dc.raft.rpc.GrpcClient;

public class BootStrap extends RemoteBootStrap{

    private GrpcClient grpcClient;

    @Override
    public void start() throws Exception {
        super.start();

    }

    @Override
    public void stop() {

    }
}
