package net.jeffpoole.httpserver.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;

import net.jeffpoole.httpserver.logic.HttpServer;
import net.jeffpoole.httpserver.parsing.HttpRequest;


/**
 * User: jpoole Date: 9/13/15 Time: 6:57 PM
 */
@Slf4j

public class NettyHttpServerInboundHandler extends SimpleChannelInboundHandler<HttpRequest>
{
  final HttpServer httpServer;
  /*
    From the HawtDispatch documentation (which has been offline forever):
    http://web.archive.org/web/20140714202719/http://hawtdispatch.fusesource.org/

         Serial Dispatch Queue: Execute the submitted runnable tasks in FIFO order. A serial dispatch
         queue will only invoke one runnable at a time, but independent queues may each execute their
         runnable objects concurrently with respect to each other. Serial dispatch queues are created
         by the application using the Dispatch.createQueue method.

    So this will be used so we can queue up multiple requests on one connection and still be sure
    the responses get returned in-order.  But requests from different connections will be processed
    in parallel.
   */
  DispatchQueue queue = Dispatch.createQueue();

  public NettyHttpServerInboundHandler(HttpServer httpServer)
  {
    super(false); // HttpRequest isn't reference-counted, so no need to try to free it
    this.httpServer = httpServer;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final HttpRequest msg)
      throws Exception
  {
    queue.execute( () -> ctx.channel().writeAndFlush(httpServer.respond(msg)));
  }
}
