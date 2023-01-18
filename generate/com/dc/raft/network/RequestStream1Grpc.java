package com.dc.raft.network;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.26.0)",
    comments = "Source: request.proto")
public final class RequestStream1Grpc {

  private RequestStream1Grpc() {}

  public static final String SERVICE_NAME = "RequestStream1";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.dc.raft.network.Payload,
      com.dc.raft.network.Payload> getRequestStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "requestStream",
      requestType = com.dc.raft.network.Payload.class,
      responseType = com.dc.raft.network.Payload.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.dc.raft.network.Payload,
      com.dc.raft.network.Payload> getRequestStreamMethod() {
    io.grpc.MethodDescriptor<com.dc.raft.network.Payload, com.dc.raft.network.Payload> getRequestStreamMethod;
    if ((getRequestStreamMethod = RequestStream1Grpc.getRequestStreamMethod) == null) {
      synchronized (RequestStream1Grpc.class) {
        if ((getRequestStreamMethod = RequestStream1Grpc.getRequestStreamMethod) == null) {
          RequestStream1Grpc.getRequestStreamMethod = getRequestStreamMethod =
              io.grpc.MethodDescriptor.<com.dc.raft.network.Payload, com.dc.raft.network.Payload>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "requestStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dc.raft.network.Payload.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dc.raft.network.Payload.getDefaultInstance()))
              .setSchemaDescriptor(new RequestStream1MethodDescriptorSupplier("requestStream"))
              .build();
        }
      }
    }
    return getRequestStreamMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RequestStream1Stub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RequestStream1Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RequestStream1Stub>() {
        @java.lang.Override
        public RequestStream1Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RequestStream1Stub(channel, callOptions);
        }
      };
    return RequestStream1Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RequestStream1BlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RequestStream1BlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RequestStream1BlockingStub>() {
        @java.lang.Override
        public RequestStream1BlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RequestStream1BlockingStub(channel, callOptions);
        }
      };
    return RequestStream1BlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RequestStream1FutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RequestStream1FutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RequestStream1FutureStub>() {
        @java.lang.Override
        public RequestStream1FutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RequestStream1FutureStub(channel, callOptions);
        }
      };
    return RequestStream1FutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class RequestStream1ImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Sends a biStreamRequest
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.dc.raft.network.Payload> requestStream(
        io.grpc.stub.StreamObserver<com.dc.raft.network.Payload> responseObserver) {
      return asyncUnimplementedStreamingCall(getRequestStreamMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getRequestStreamMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                com.dc.raft.network.Payload,
                com.dc.raft.network.Payload>(
                  this, METHODID_REQUEST_STREAM)))
          .build();
    }
  }

  /**
   */
  public static final class RequestStream1Stub extends io.grpc.stub.AbstractAsyncStub<RequestStream1Stub> {
    private RequestStream1Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RequestStream1Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RequestStream1Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Sends a biStreamRequest
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.dc.raft.network.Payload> requestStream(
        io.grpc.stub.StreamObserver<com.dc.raft.network.Payload> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getRequestStreamMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   */
  public static final class RequestStream1BlockingStub extends io.grpc.stub.AbstractBlockingStub<RequestStream1BlockingStub> {
    private RequestStream1BlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RequestStream1BlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RequestStream1BlockingStub(channel, callOptions);
    }
  }

  /**
   */
  public static final class RequestStream1FutureStub extends io.grpc.stub.AbstractFutureStub<RequestStream1FutureStub> {
    private RequestStream1FutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RequestStream1FutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RequestStream1FutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_REQUEST_STREAM = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RequestStream1ImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RequestStream1ImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REQUEST_STREAM:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.requestStream(
              (io.grpc.stub.StreamObserver<com.dc.raft.network.Payload>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class RequestStream1BaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RequestStream1BaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.dc.raft.network.Request.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RequestStream1");
    }
  }

  private static final class RequestStream1FileDescriptorSupplier
      extends RequestStream1BaseDescriptorSupplier {
    RequestStream1FileDescriptorSupplier() {}
  }

  private static final class RequestStream1MethodDescriptorSupplier
      extends RequestStream1BaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    RequestStream1MethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RequestStream1Grpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RequestStream1FileDescriptorSupplier())
              .addMethod(getRequestStreamMethod())
              .build();
        }
      }
    }
    return result;
  }
}
