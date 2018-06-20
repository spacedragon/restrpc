package io.insight.http;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.insight.restrpc.Context;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class UndertowHandlerContext<RespT extends Message> implements Context<RespT> {
  private HttpServerExchange exchange;

  public UndertowHandlerContext(HttpServerExchange exchange) {
    this.exchange = exchange;
  }

  public HttpServerExchange exchange() {
    return this.exchange;
  }

  @Override
  public void done() {
    exchange.endExchange();
  }

  @Override
  public void status(int code, String... messages) {
    exchange.setStatusCode(code);
    for (String message : messages) {
      exchange.getResponseSender().send(message);
    }
    exchange.endExchange();
  }

  @Override
  public void ok(String... messages) {
     status(200,messages);
  }

  @Override
  public void ok(RespT response) {
    status(200, response);
  }

  public void status(int code, RespT response) {
    try {
      exchange.setStatusCode(code);


      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
      exchange.getResponseSender().send(JsonFormat.printer().print(response));
    } catch (Exception e) {
      exchange.setStatusCode(500);
    }
    exchange.endExchange();
  }


}
