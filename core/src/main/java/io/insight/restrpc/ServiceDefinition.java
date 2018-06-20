package io.insight.restrpc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServiceDefinition {

  private final String serviceName;
  private final Map<String, MethodDefinition> methods;


  public ServiceDefinition(String serviceName, Map<String, MethodDefinition<?, ?>> methods) {
    this.serviceName = serviceName;
    this.methods = Collections.unmodifiableMap(methods);
  }

  public static Builder builder(String serviceName) {
    return new Builder(serviceName);
  }

  public Collection<MethodDefinition> methods() {
    return methods.values();
  }

  public MethodDefinition getMethod(String methodName) {
    return methods.get(methodName);
  }

  public static class Builder {
    private String serviceName;

    private final Map<String, MethodDefinition<?, ?>> methods =
        new HashMap<>();

    public Builder(String serviceName) {
      this.serviceName = serviceName;
    }

    public <Req, Resp> Builder addMethod(io.grpc.MethodDescriptor<Req, Resp> methodDescriptor,
                                              ServerCallHandler<Req, Resp> serverCallHandler) {
      MethodDefinition<Req, Resp> method = MethodDefinition.create(methodDescriptor, serverCallHandler);
      methods.put(methodDescriptor.getFullMethodName(), method);
      return this;
    }

    public ServiceDefinition build() {
      return new ServiceDefinition(serviceName, methods);
    }
  }
}
