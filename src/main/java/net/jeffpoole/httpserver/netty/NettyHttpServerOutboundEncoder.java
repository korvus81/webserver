package net.jeffpoole.httpserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Charsets;

import net.jeffpoole.httpserver.data.Blob;
import net.jeffpoole.httpserver.data.ByteArrayBlob;
import net.jeffpoole.httpserver.data.FileBlob;
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
    //InputStream dataStream = httpResponse.getResource().getData().get();
    Blob blob = httpResponse.getResource().getData();
    if (blob != null)
    {
      if (blob instanceof ByteArrayBlob) {
        byte[] data = ((ByteArrayBlob) blob).getBytes();
        byteBuf.writeBytes(data);
      }
      else if (blob instanceof FileBlob) {

        File file = ((FileBlob) blob).getFile();
        byteBuf.writeBytes(new FileInputStream(file), (int)file.length());

        // TODO: Try using zero-copy option -- I'm leaving it out for now since it seems to be
        // slower for small files
        /*FileRegion fr = new DefaultFileRegion(file, 0, file.length());
        channelHandlerContext.write(byteBuf);
        channelHandlerContext.writeAndFlush(fr);*/
      }

      log.debug("Done writing data");
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
