package net.jeffpoole.httpserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Charsets;

import net.jeffpoole.httpserver.parsing.HttpResponse;


/**
 * User: jpoole Date: 9/13/15 Time: 6:57 PM
 */
@Slf4j
public class NettyHttpServerOutboundEncoder extends MessageToByteEncoder<HttpResponse>
{

  @Override
  protected void encode(final ChannelHandlerContext channelHandlerContext,
      final HttpResponse httpResponse,
      final ByteBuf byteBuf) throws Exception
  {
    String responseStatus =
        httpResponse.getHttpVersion() + " "
            + httpResponse.getStatusCode() + " "
            + httpResponse.getStatusReason() + "\r\n";
    byteBuf.writeBytes(responseStatus.getBytes(Charsets.UTF_8));

    for (String headerName : httpResponse.getHeaders().keySet())
    {
      String headerLine = headerName + ": " + httpResponse.getHeaders().get(headerName) + "\r\n";
      byteBuf.writeBytes(headerLine.getBytes(Charsets.UTF_8));
    }
    byteBuf.writeBytes("\r\n".getBytes(Charsets.UTF_8));
    log.debug("Done writing headers");
    InputStream dataStream = httpResponse.getResource().getDataStream().get();
    if (httpResponse.getResource().isPresent()
        && dataStream != null)
    {
      // TODO: replace with http://netty.io/4.0/api/io/netty/channel/DefaultFileRegion.html implementation
      while (dataStream.available() > 0)
        byteBuf.writeBytes(dataStream, dataStream.available());
      dataStream.close();
      log.debug("Done writing data");
      //byteBuf.writeBytes("\r\n".getBytes(Charsets.UTF_8));
    }
    else log.debug("No data to write");

    if ("close".equals(httpResponse.getHeaders().getOrDefault("Connection", ""))) {
      log.debug("Closing connection");
      // Schedule slightly in the future, since I believe there may be a race condition between the
      // data getting to the socket and the closing of the socket.  TODO: find a better way
      channelHandlerContext.executor().schedule(
          () -> channelHandlerContext.channel().close(),
          1, TimeUnit.MILLISECONDS);
    }
  }
}
