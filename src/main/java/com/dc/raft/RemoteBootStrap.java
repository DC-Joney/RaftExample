package com.dc.raft;

import com.dc.raft.annotation.RpcHandler;
import com.dc.raft.handler.RequestHandler;
import com.dc.raft.handler.RequestHandlerAcceptor;
import com.dc.raft.node.RaftNode;
import com.dc.raft.node.RaftPeers;
import com.google.common.collect.Sets;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;

public abstract class RemoteBootStrap implements LifeCycle {

    private RaftNode raftNode;

    /**
     * 所有的Raft 节点
     */
    protected final RaftPeers raftPeers;

    public RemoteBootStrap(RaftPeers.PeerNode self, Set<RaftPeers.PeerNode> others) {
        raftPeers = new RaftPeers(self);
        raftPeers.addPeers(others);
    }

    @Override
    public void start() throws Exception {
        this.raftNode = new RaftNode();
        Set<RequestHandler> requestHandlers = scanHandler(raftNode);
        initClient(requestHandlers);

        //启动当前节点
        raftNode.setRaftPeers(raftPeers);
        raftNode.start();
    }

    protected abstract void initClient(Set<RequestHandler> requestHandlers);


    @Override
    public void stop() {
        raftNode.stop();
    }

    public Set<RequestHandler> scanHandler(RaftNode raftNode) throws Exception {
        Set<Class<?>> handlerClasses = AnnotationScans.scan(RpcHandler.class, RequestHandler.class, scanPackageHandlers().toArray(new String[0]));
        Set<RequestHandler> requestHandlerSets = Sets.newHashSet();
        for (Class<?> handlerClass : handlerClasses) {
            boolean defaultConstructor = false;
            Constructor<?> instanceConstructor = ClassUtils.getConstructorIfAvailable(handlerClass, RaftNode.class);
            //如果没有对应的构造器则获取默认的构造器
            if (instanceConstructor == null) {
                instanceConstructor = ClassUtils.getConstructorIfAvailable(handlerClass);
                defaultConstructor = true;
            }

            if (instanceConstructor == null) {
                throw new RuntimeException("Cannot resolve default constructor");
            }

            //通过构造器创建相应的RequestHandler，保证单例
            RequestHandler requestHandler = defaultConstructor ? (RequestHandler) BeanUtils.instantiateClass(instanceConstructor)
                    : (RequestHandler) BeanUtils.instantiateClass(instanceConstructor, raftNode);

            requestHandlerSets.add(requestHandler);
        }
        return requestHandlerSets;
    }


    public RaftNode getRaftNode() {
        return raftNode;
    }

    protected abstract Set<String> scanPackageHandlers();
}
