package com.dc.raft.handler;

import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.network.Metadta;
import com.dc.raft.node.RaftNode;

public interface RequestHandler {

    /**
     * 用于处理客户端的请求，并返回处理结果
     * @param request 客户端请求
     */
    ResponseCommand handle(RequestCommand request);


    /**
     * 判断当前RequestHandler 是否支持对应的 type
     * @param metadata 元数据
     */
    Class<?> supportType();

}
