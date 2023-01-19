package com.dc.raft.rpc;

import com.dc.raft.LifeCycle;
import com.dc.raft.handler.RequestHandlerAcceptor;
import com.dc.raft.network.Payload;
import com.dc.raft.network.RaftRequestGrpc;
import io.grpc.*;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ServerCalls;
import io.grpc.util.MutableHandlerRegistry;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Rpc Server 用于接受RPC请求处理
 *
 * @author zhangyang
 */
@Slf4j
public class GrpcServer implements LifeCycle {

    private Server server;

    private final int port;

    /**
     * 用于接受Rpc请求
     */
    private RequestHandlerAcceptor handlerAcceptor;

    public static final Attributes.Key<String> REMOTE_IP = Attributes.Key.create("remote-ip");
    public static final Attributes.Key<String> REMOTE_PORT = Attributes.Key.create("remote-port");
    public static final Attributes.Key<String> LOCAL_PORT = Attributes.Key.create("local-port");
    public static final Attributes.Key<String> LOCAL_IP = Attributes.Key.create("local-ip");

    public GrpcServer(int port) {
        this.port = port;
    }

    @Override
    public void start() {
        //启动Rpc Server
        startServer();
    }

    /**
     * 设置接受处理器
     * @param handlerAcceptor
     */
    public void setHandlerAcceptor(RequestHandlerAcceptor handlerAcceptor) {
        this.handlerAcceptor = handlerAcceptor;
    }

    private void startServer() {
        try {
            MutableHandlerRegistry mutableHandlerRegistry = new MutableHandlerRegistry();
            addService(mutableHandlerRegistry);
            this.server = ServerBuilder.forPort(port)
                    .fallbackHandlerRegistry(mutableHandlerRegistry)
                    .addTransportFilter(new ServerTransportFilter() {
                        @Override
                        public Attributes transportReady(Attributes transportAttrs) {
                            InetSocketAddress remoteAddress = (InetSocketAddress) transportAttrs.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
                            InetSocketAddress localAddress = (InetSocketAddress) transportAttrs.get(Grpc.TRANSPORT_ATTR_LOCAL_ADDR);
                            String remotePort = String.valueOf(remoteAddress.getPort());
                            String localPort = String.valueOf(localAddress.getPort());
                            return transportAttrs.toBuilder().set(REMOTE_IP, remoteAddress.getHostName())
                                    .set(REMOTE_PORT, remotePort)
                                    .set(LOCAL_IP, localAddress.getHostName())
                                    .set(LOCAL_PORT, localPort).build();
                        }
                    })
                    .build();
            server.start();
            log.info("Server start port is : {}", port);
        } catch (IOException e) {
            throw new RuntimeException("server start error");
        }
    }


    private void addService(MutableHandlerRegistry handlerRegistry) {

        MethodDescriptor<Payload, Payload> descriptor = MethodDescriptor.<Payload, Payload>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(RaftRequestGrpc.getRequestMethod().getFullMethodName())
                .setIdempotent(true)
                .setRequestMarshaller(ProtoUtils.marshaller(Payload.getDefaultInstance()))
                .setResponseMarshaller(ProtoUtils.marshaller(Payload.getDefaultInstance()))
                .build();

        //设置在调用rpc调用时，具体的处理
        ServerCallHandler<Payload, Payload> serverCallHandler = ServerCalls.asyncUnaryCall(handlerAcceptor::handleRequest);

        ServerServiceDefinition definition = ServerServiceDefinition.builder(RaftRequestGrpc.SERVICE_NAME)
                .addMethod(descriptor, serverCallHandler)
                .build();

        ServerServiceDefinition interceptDefinition = ServerInterceptors.intercept(definition);
        handlerRegistry.addService(interceptDefinition);
    }

    @Override
    public void stop() {
        server.shutdown();
    }


}
