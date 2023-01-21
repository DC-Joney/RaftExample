package com.dc.raft.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCode;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.network.Metadta;
import com.dc.raft.network.Payload;
import com.dc.raft.rpc.PayloadRegistry;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.Collection;

/**
 * 用于处理rpc请求，并且返回response
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestHandlerAcceptor {

    final Collection<RequestHandler> requestHandlers;

    public RequestHandlerAcceptor(Collection<RequestHandler> requestHandlers) {
        this.requestHandlers = requestHandlers;
    }

    /**
     * 添加请求处理器
     */
    public void addRequestHandler(RequestHandler requestHandler) {
        this.requestHandlers.add(requestHandler);
    }

    public void handleRequest(Payload request, StreamObserver<Payload> responseObserver) {
        Metadta metadta = request.getMetadta();
        try {
            RequestCommand requestCommand = parseRequest(request);
            ResponseCommand responseCommand = requestHandlers.stream()
                    .filter(requestHandler -> support(requestHandler, metadta))
                    .findFirst()
                    .map(handler -> handler.handle(requestCommand))
                    .orElseGet(this::notFindHandlerCommand);

            Payload payload = convertResponse(responseCommand);
            responseObserver.onNext(payload);
        } catch (Exception e) {
            log.error("Handle requestCommand error, requestType is: {}, error is:", metadta.getType(), e);
            ResponseCommand responseCommand = getErrorResponseCommand("Handler request error, cause is:", e.getMessage());
            Payload payload = convertResponse(responseCommand);
            responseObserver.onNext(payload);
        }

        responseObserver.onCompleted();
    }

    private boolean support(RequestHandler requestHandler, Metadta metadta) {
        return requestHandler.supportType().getSimpleName().equals(metadta.getType());
    }

    private ResponseCommand notFindHandlerCommand() {
        ErrorResponseCommand responseCommand = new ErrorResponseCommand();
        responseCommand.setErrorInfo(ResponseCode.FAIL.getCode(), "Cannot find requestHandler, please check it");
        return responseCommand;
    }

    private ResponseCommand getErrorResponseCommand(String message, Object... args) {
        ErrorResponseCommand responseCommand = new ErrorResponseCommand();
        responseCommand.setErrorInfo(ResponseCode.FAIL.getCode(), StrUtil.format(message, args));
        return responseCommand;
    }

    protected <T extends RequestCommand> T parseRequest(Payload payload) {
        ByteString bytes = payload.getBody();
        Metadta metadta = payload.getMetadta();
        Class<?> jsonClass = PayloadRegistry.getClassByType(metadta.getType());
        if (RequestCommand.class.isAssignableFrom(jsonClass)) {
            T request = JSONObject.parseObject(bytes.toByteArray(), jsonClass);
            request.putAllHeader(metadta.getHeadersMap());
            return request;
        }

        throw new UnsupportedOperationException("Cannot serialize class: " + jsonClass.getCanonicalName());
    }

    private Payload convertResponse(ResponseCommand responseCommand) {
        Metadta metadta = Metadta.newBuilder()
                .setType(responseCommand.getClass().getSimpleName()).build();

        String jsonStr = JSON.toJSONString(responseCommand);

        ByteString bytes = ByteString.copyFrom(jsonStr, Charset.defaultCharset());
        return Payload.newBuilder()
                .setMetadta(metadta)
                .setBody(bytes).build();
    }

    public static class ErrorResponseCommand extends ResponseCommand {


    }
}
