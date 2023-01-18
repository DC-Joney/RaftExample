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
public final class RaftRequestGrpc {

  private RaftRequestGrpc() {}

  public static final String SERVICE_NAME = "RaftRequest";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.dc.raft.network.Payload,
      com.dc.raft.network.Payload> getRequestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "request",
      requestType = com.dc.raft.network.Payload.class,
      responseType = com.dc.raft.network.Payload.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.dc.raft.network.Payload,
      com.dc.raft.network.Payload> getRequestMethod() {
    io.grpc.MethodDescriptor<com.dc.raft.network.Payload, com.dc.raft.network.Payload> getRequestMethod;
    if ((getRequestMethod = RaftRequestGrpc.getRequestMethod) == null) {
      synchronized (RaftRequestGrpc.class) {
        if ((getRequestMethod = RaftRequestGrpc.getRequestMethod) == null) {
          RaftRequestGrpc.getRequestMethod = getRequestMethod =
              io.grpc.MethodDescriptor.<com.dc.raft.network.Payload, com.dc.raft.network.Payload>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "request"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dc.raft.network.Payload.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.dc.raft.network.Payload.getDefaultInstance()))
              .setSchemaDescriptor(new RaftRequestMethodDescriptorSupplier("request"))
              .build();
        }
      }
    }
    return getRequestMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RaftRequestStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RaftRequestStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RaftRequestStub>() {
        @java.lang.Override
        public RaftRequestStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RaftRequestStub(channel, callOptions);
        }
      };
    return RaftRequestStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RaftRequestBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RaftRequestBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RaftRequestBlockingStub>() {
        @java.lang.Override
        public RaftRequestBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RaftRequestBlockingStub(channel, callOptions);
        }
      };
    return RaftRequestBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RaftRequestFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RaftRequestFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RaftRequestFutureStub>() {
        @java.lang.Override
        public RaftRequestFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RaftRequestFutureStub(channel, callOptions);
        }
      };
    return RaftRequestFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class RaftRequestImplBase implements io.grpc.BindableService {

    /**
     */
    public void request(com.dc.raft.network.Payload request,
        io.grpc.stub.StreamObserver<com.dc.raft.network.Payload> responseObserver) {
      asyncUnimplementedUnaryCall(getRequestMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getRequestMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.dc.raft.network.Payload,
                com.dc.raft.network.Payload>(
                  this, METHODID_REQUEST)))
          .build();
    }
  }

  /**
   */
  public static final class RaftRequestStub extends io.grpc.stub.AbstractAsyncStub<RaftRequestStub> {
    private RaftRequestStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RaftRequestStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RaftRequestStub(channel, callOptions);
    }

    /**
     */
    public void request(com.dc.raft.network.Payload request,
        io.grpc.stub.StreamObserver<com.dc.raft.network.Payload> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRequestMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class RaftRequestBlockingStub extends io.grpc.stub.AbstractBlockingStub<RaftRequestBlockingStub> {
    private RaftRequestBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RaftRequestBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RaftRequestBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.dc.raft.network.Payload request(com.dc.raft.network.Payload request) {
      return blockingUnaryCall(
          getChannel(), getRequestMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class RaftRequestFutureStub extends io.grpc.stub.AbstractFutureStub<RaftRequestFutureStub> {
    private RaftRequestFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RaftRequestFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RaftRequestFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.dc.raft.network.Payload> request(
        com.dc.raft.network.Payload request) {
      return futureUnaryCall(
          getChannel().newCall(getRequestMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REQUEST = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RaftRequestImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RaftRequestImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REQUEST:
          serviceImpl.request((com.dc.raft.network.Payload) request,
              (io.grpc.stub.StreamObserver<com.dc.raft.network.Payload>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class RaftRequestBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RaftRequestBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.dc.raft.network.Request.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RaftRequest");
    }
  }

  private static final class RaftRequestFileDescriptorSupplier
      extends RaftRequestBaseDescriptorSupplier {
    RaftRequestFileDescriptorSupplier() {}
  }

  private static final class RaftRequestMethodDescriptorSupplier
      extends RaftRequestBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    RaftRequestMethodDescriptorSupplier(String methodName) {
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
      synchronized (RaftRequestGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RaftRequestFileDescriptorSupplier())
              .addMethod(getRequestMethod())
              .build();
        }
      }
    }
    return result;
  }
}
