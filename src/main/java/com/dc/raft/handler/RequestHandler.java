package com.dc.raft.handler;

import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.network.Metadta;

public interface RequestHandler {

    /**
     * 用于处理客户端的请求，并返回处理结果
     * @param request 客户端请求
     */
    ResponseCommand handle(RequestCommand request);


    boolean support(Metadta metadta);


}
