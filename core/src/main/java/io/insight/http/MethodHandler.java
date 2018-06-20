package io.insight.http;

import com.google.api.HttpRule;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import io.grpc.MethodDescriptor;
import io.insight.restrpc.MethodDefinition;
import io.insight.utils.StringUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.PathTemplateMatch;

import java.util.Map;

public class MethodHandler implements HttpHandler {
  private MethodDefinition<Object, Object> method;
  private HttpRule httpRule;

  public MethodHandler(MethodDefinition<Object, Object> method, HttpRule httpRule) {
    this.method = method;
    this.httpRule = httpRule;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    MethodDescriptor<Object,Object> descriptor = method.getMethod();

    MethodDescriptor.PrototypeMarshaller requestMarshaller =
        ((MethodDescriptor.PrototypeMarshaller) descriptor.getRequestMarshaller());
    GeneratedMessageV3 requestPrototype = (GeneratedMessageV3) (requestMarshaller.getMessagePrototype());
    Message.Builder requestBuilder = requestPrototype.toBuilder();
    if (this.httpRule.getBody() != null) {
      if (exchange.isInIoThread()) {
        exchange.dispatch(this);
        return;
      }
      exchange.startBlocking();
      String body = StringUtils.is2str(exchange.getInputStream());
      if ("*".equals(this.httpRule.getBody())) {
        JsonFormat.parser().ignoringUnknownFields().merge(body, requestBuilder);
      } else {
        Descriptors.FieldDescriptor field = requestBuilder.getDescriptorForType().findFieldByName(this.httpRule.getBody());
        if (field != null) {
          setFieldValue(requestBuilder, field, body);
        }
      }
    }
    PathTemplateMatch attachment = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
    Map<String, String> params = attachment.getParameters();
    for (Map.Entry<String, String> entry : params.entrySet()) {
      Descriptors.FieldDescriptor field = requestBuilder.getDescriptorForType().findFieldByName(entry.getKey());
      if (field != null) {
        setFieldValue(requestBuilder, field, entry.getValue());
      }
    }

    method.getHandler().call(requestBuilder.build(), new UndertowHandlerContext(exchange));
  }

  private void setFieldValue(Message.Builder target, Descriptors.FieldDescriptor field, String value)
      throws InvalidProtocolBufferException {
    switch (field.getJavaType()) {
      case STRING:
        target.setField(field, value);
      case INT:
        target.setField(field, Integer.valueOf(value));
      case LONG:
        target.setField(field, Long.valueOf(value));
      case FLOAT:
        target.setField(field, Float.valueOf(value));
      case DOUBLE:
        target.setField(field, Double.valueOf(value));
      case BOOLEAN:
        target.setField(field, Boolean.valueOf(value));
      case BYTE_STRING:
        target.setField(field, ByteString.copyFromUtf8(value));
      case ENUM:
        Descriptors.EnumValueDescriptor enumValue = field.getEnumType().findValueByName(value);
        target.setField(field, enumValue);
      case MESSAGE:
        Message.Builder fieldBuilder = target.getFieldBuilder(field);
        JsonFormat.Parser parser = JsonFormat.parser().ignoringUnknownFields();
        parser.merge(value, fieldBuilder);
        target.setField(field, fieldBuilder.build());
    }
  }


}
