package com.dc.raft.rpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dc.raft.Requester;
import com.dc.raft.LifeCycle;
import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.exception.RaftException;
import com.dc.raft.exception.RaftSerializeException;
import com.dc.raft.network.Metadta;
import com.dc.raft.network.Payload;
import com.dc.raft.network.RaftRequestGrpc;
import com.google.protobuf.ByteString;
import io.grpc.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GrpcClient implements LifeCycle, Requester {

    static final CallOptions.Key<InetSocketAddress> REQUEST_ADDRESS = CallOptions.Key.create("request-address");

    ManagedChannel managedChannel;

    /**
     * 远程RPC的地址
     */
    final InetSocketAddress socketAddress;

    @Getter
    @Setter
    volatile RaftRequestGrpc.RaftRequestFutureStub requestStub;

    public GrpcClient(InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    @Override
    public void start() {
        managedChannel = ManagedChannelBuilder
                .forAddress(socketAddress.getHostName(), socketAddress.getPort())
                .usePlaintext()
                .intercept(new ClientInterceptor() {
                    @Override
                    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                        callOptions = callOptions.withOption(REQUEST_ADDRESS, socketAddress);
                        return next.newCall(method, callOptions);
                    }
                })
                .enableRetry()
                .build();

        requestStub = RaftRequestGrpc.newFutureStub(managedChannel);
    }

    @Override
    public <T extends ResponseCommand> T request(RequestCommand request) {
        return request(request, Duration.ofSeconds(5));
    }

    @Override
    public <T extends ResponseCommand> T request(RequestCommand request, Duration timeout) {
        Payload response;
        try {
            Payload payload = convertRequest(request);
            log.info("rpc request start, request is: {}", request);
            response = requestStub.request(payload).get(timeout.getSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Request server error,cause is: ", e);
            throw new RaftException("Request server error,cause is: ", e);
        }

        return parseResponse(response);
    }


    /**
     * 将payload解析为对应的ResponseCommand
     *
     * @param payload response
     */
    private <T extends ResponseCommand> T parseResponse(Payload payload) {
        ByteString bytes = payload.getBody();
        Metadta metadta = payload.getMetadta();
        Class<?> jsonClass = PayloadRegistry.getClassByType(metadta.getType());
        if (ResponseCommand.class.isAssignableFrom(jsonClass)) {
            return JSONObject.parseObject(bytes.toByteArray(), jsonClass);
        }

        throw new RaftSerializeException("Cannot serialize class: {}", jsonClass.getCanonicalName());
    }


    /**
     * 将request 转换为rpc对应的 {@link Payload}
     * @param request 请求信息
     */
    private Payload convertRequest(RequestCommand request) {
        Map<String, String> headers = request.getHeaders();
        Metadta metadta = Metadta.newBuilder()
                .setType(request.getClass().getSimpleName())
                .setClientIp(socketAddress.toString())
                .putAllHeaders(headers).build();

        String jsonStr = JSON.toJSONString(request);

        ByteString bytes = ByteString.copyFrom(jsonStr, Charset.defaultCharset());
        return Payload.newBuilder()
                .setMetadta(metadta)
                .setBody(bytes).build();
    }

    @Override
    public void stop() {
        managedChannel.shutdown();
    }


}
