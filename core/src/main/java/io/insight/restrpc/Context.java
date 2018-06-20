package io.insight.restrpc;


import io.undertow.server.HttpServerExchange;

public interface Context<RespT> {

  HttpServerExchange exchange();

  void done();

  void status(int code, String... messages);

  void ok(String... messages);

  void ok(RespT response);

  void status(int code, RespT response);
}
