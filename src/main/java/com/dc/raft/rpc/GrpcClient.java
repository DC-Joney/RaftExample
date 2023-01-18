package com.dc.raft.rpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dc.Requester;
import com.dc.raft.LifeCycle;
import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.network.Metadta;
import com.dc.raft.network.Payload;
import com.dc.raft.network.RaftRequestGrpc;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class GrpcClient implements LifeCycle, Requester {

    ManagedChannel managedChannel;

    /**
     * 远程RPC的地址
     */
    final InetSocketAddress socketAddress;

    RaftRequestGrpc.RaftRequestFutureStub requestStub;

    public GrpcClient(InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    @Override
    public void start() {
        managedChannel = ManagedChannelBuilder
                .forAddress(socketAddress.getHostName(), socketAddress.getPort()).build();

        requestStub = RaftRequestGrpc.newFutureStub(managedChannel);
    }


    @Override
    public <T extends ResponseCommand> T request(RequestCommand request, Duration timeout) {
        Payload payload = convertRequest(request);
        Payload response;
        try {
            response = requestStub.request(payload).get(timeout.getSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Server error");
        }
        return parseResponse(response);
    }


    private <T extends ResponseCommand> T parseResponse(Payload payload) {
        ByteString bytes = payload.getBody();
        Metadta metadta = payload.getMetadta();
        Class<?> jsonClass = PayloadRegistry.getClassByType(metadta.getType());
        if (ResponseCommand.class.isAssignableFrom(jsonClass)) {
            return JSONObject.parseObject(bytes.toByteArray(), jsonClass);
        }

        throw new UnsupportedOperationException("Cannot serialize class: " + jsonClass.getCanonicalName());
    }


    private Payload convertRequest(RequestCommand request) {
        Map<String, String> headers = request.getHeaders();
        Metadta metadta = Metadta.newBuilder()
                .setType(request.getClass().getSimpleName())
                .setClientIp(request.getSocketAddress().toString())
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
