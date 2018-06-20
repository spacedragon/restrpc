package io.insight.restrpc;

import com.google.protobuf.GeneratedMessageV3;
import io.grpc.MethodDescriptor;

public class MethodDefinition<ReqT , RespT > {
  private final MethodDescriptor<ReqT, RespT> method;
  private final ServerCallHandler<ReqT, RespT> handler;

  private MethodDefinition(MethodDescriptor<ReqT, RespT> method,
                                 ServerCallHandler<ReqT, RespT> handler) {
    this.method = method;
    this.handler = handler;
  }

  public static <ReqT, RespT> MethodDefinition<ReqT, RespT> create(
      MethodDescriptor<ReqT, RespT> method,
      ServerCallHandler<ReqT,RespT> handler) {
    return new MethodDefinition<ReqT, RespT>(method, handler);
  }

  public MethodDescriptor<ReqT, RespT> getMethod() {
    return method;
  }

  public ServerCallHandler<ReqT, RespT> getHandler() {
    return handler;
  }
}
