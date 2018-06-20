import io.grpc.examples.helloworld.GreeterRestRpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.insight.http.UndertowHandler;
import io.insight.restrpc.Context;
import io.undertow.Undertow;

public class Main {


  static class HelloImpl extends GreeterRestRpc.GreeterImplBase {
    @Override
    public void sayHello(HelloRequest request, Context<HelloReply> context) {
      context.ok(HelloReply.newBuilder().setMessage("hello " + request.getName()).build());
    }
  }


  public static void main(String[] args) {
    Undertow server = Undertow.builder()
        .addHttpListener(8080, "localhost")
        .setHandler(new UndertowHandler(new HelloImpl()))
        .build();
    server.start();
  }
}
