package io.insight.http;

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.protobuf.Descriptors;
import io.grpc.protobuf.ProtoMethodDescriptorSupplier;
import io.insight.restrpc.BindableService;
import io.insight.restrpc.MethodDefinition;
import io.insight.restrpc.ServiceDefinition;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.AllowedMethodsHandler;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.util.HttpString;

public class UndertowHandler implements HttpHandler {

  private final PathTemplateHandler routeHandler;
  private BindableService[] services;

  public UndertowHandler(BindableService... services) {
    this.services = services;
    this.routeHandler = new PathTemplateHandler();
    bindServices();
  }

  private void bindServices() {
    for (BindableService service : services) {
      ServiceDefinition definition = service.bindService();
      for (MethodDefinition method : definition.methods()) {
        Descriptors.MethodDescriptor detailDescriptor =
            ((ProtoMethodDescriptorSupplier) method.getMethod().getSchemaDescriptor()).getMethodDescriptor();
        HttpRule httpRule = detailDescriptor.getOptions().getExtension(AnnotationsProto.http);

        AllowedMethodsHandler handler = new AllowedMethodsHandler(
            new MethodHandler(method, httpRule), HttpString.tryFromString(httpRule.getPatternCase().name()));
        switch (httpRule.getPatternCase()) {
          case GET:
            routeHandler.add(httpRule.getGet(), handler);
            break;
          case PUT:
            routeHandler.add(httpRule.getPut(), handler);
            break;
          case POST:
            routeHandler.add(httpRule.getPost(), handler);
            break;
          case DELETE:
            routeHandler.add(httpRule.getDelete(), handler);
            break;
          case PATCH:
            routeHandler.add(httpRule.getPatch(), handler);
            break;
          default:
            throw new IllegalArgumentException();
        }
      }
    }
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    this.routeHandler.handleRequest(exchange);
  }
}
