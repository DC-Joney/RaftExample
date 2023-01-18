package com.dc.raft.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.network.Metadta;
import com.dc.raft.network.Payload;
import com.dc.raft.rpc.PayloadRegistry;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于处理rpc请求，并且返回response
 */
public class RequestHandlerAcceptor  {

    List<RequestHandler> requestHandlers = new ArrayList<>();

    /**
     * 添加请求处理器
     */
    public void addRequestHandler(RequestHandler requestHandler) {
        this.requestHandlers.add(requestHandler);
    }

    public void handleRequest(Payload request, StreamObserver<Payload> responseObserver) {
        Metadta metadta = request.getMetadta();
        String type = metadta.getType();
        RequestCommand requestCommand = parseRequest(request);

        ResponseCommand responseCommand = requestHandlers.stream()
                .filter(requestHandler -> requestHandler.support(metadta))
                .findFirst()
                .map(handler-> handler.handle(requestCommand))
                .orElse(new ResponseCommand() {});

        Payload payload = convertResponse(responseCommand);

        responseObserver.onNext(payload);
        responseObserver.onCompleted();
    }

    protected  <T extends RequestCommand> T parseRequest(Payload payload) {
        ByteString bytes = payload.getBody();
        Metadta metadta = payload.getMetadta();
        Class<?> jsonClass = PayloadRegistry.getClassByType(metadta.getType());
        if (RequestCommand.class.isAssignableFrom(jsonClass)) {
            T request = JSONObject.parseObject(bytes.toByteArray(), jsonClass);
            request.putAllHeader(metadta.getHeadersMap());
        }

        throw new UnsupportedOperationException("Cannot serialize class: " + jsonClass.getCanonicalName());
    }

    private Payload convertResponse(ResponseCommand responseCommand) {
        Metadta metadta = Metadta.newBuilder()
                .setType(responseCommand.getClass().getName()).build();

        String jsonStr = JSON.toJSONString(responseCommand);

        ByteString bytes = ByteString.copyFrom(jsonStr, Charset.defaultCharset());
        return Payload.newBuilder()
                .setMetadta(metadta)
                .setBody(bytes).build();
    }
}
