package com.dc.raft;

import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.network.Metadta;

public interface ResultHandler {

    /**
     * 用于处理客户端的请求，并返回处理结果
     * @param request 客户端请求
     */
    <REQ extends RequestCommand> ResponseCommand handle(REQ request);

    /**
     * 处理器是否支持当前
     *
     * @param metadta 元数据信息
     */
    boolean support(Metadta metadta);
}
