package com.dc.raft.command;

import com.dc.raft.rpc.PayloadRegistry;

import java.io.Serializable;

public abstract class RemoteCommand implements Serializable {

    public RemoteCommand(){
        PayloadRegistry.register(this.getClass().getSimpleName(), this.getClass());
    }


}
