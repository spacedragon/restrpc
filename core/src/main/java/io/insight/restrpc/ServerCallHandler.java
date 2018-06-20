package io.insight.restrpc;

@FunctionalInterface
public interface ServerCallHandler<Req, Resp> {
   void call(Req req, Context<Resp> context);
}
