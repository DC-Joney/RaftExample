package com.dc.raft;

import com.dc.raft.annotation.RpcHandler;
import com.dc.raft.handler.RequestHandler;
import com.dc.raft.handler.RequestHandlerAcceptor;

import java.util.Set;

public abstract class RemoteBootStrap implements LifeCycle{

    protected RequestHandlerAcceptor handlerAcceptor;

    @Override
    public void start() throws Exception {
        Set<Class<?>> handlerClasses = AnnotationScans.scan(RpcHandler.class, RequestHandler.class, "");
        for (Class<?> handlerClass : handlerClasses) {
            RequestHandler requestHandler = (RequestHandler) handlerClass.newInstance();
            handlerAcceptor.addRequestHandler(requestHandler);
        }
    }



}
