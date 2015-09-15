package net.jeffpoole.httpserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
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
public class NettyHttpServerOutboundHandler extends ChannelOutboundHandlerAdapter
{
  // Testing showed that zero-copy sends are slower for small files (probably due to set-up time)
  // but much faster for large files.  4k bytes seems to be a reasonable cut-off, though further
  // testing could come up with a better value.
  private final static int MINIMUM_SIZE_FOR_ZERO_COPY_SEND = 4096;

  private final static byte[] CRLF = "\r\n".getBytes(Charsets.UTF_8);

  @Override
  public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise)
      throws Exception
  {
    if (msg instanceof HttpResponse)
    {
      final HttpResponse httpResponse = (HttpResponse) msg;
      ByteBuf byteBuf = ctx.alloc().ioBuffer();
      String responseStatus =
          httpResponse.getHttpVersion() + " "
              + httpResponse.getStatusCode() + " "
              + httpResponse.getStatusReason() + "\r\n";
      byteBuf.writeBytes(responseStatus.getBytes(Charsets.UTF_8));

      for (String headerName : httpResponse.getHeaders().keySet())
      {
        String headerLine = headerName + ": " + httpResponse.getHeaders().get(headerName);
        byteBuf.writeBytes(headerLine.getBytes(Charsets.UTF_8));
        byteBuf.writeBytes(CRLF);
      }
      byteBuf.writeBytes(CRLF);
      log.debug("Done writing headers");

      Blob blob = httpResponse.getResource().getData();
      if (blob != null)
      {
        if (blob instanceof ByteArrayBlob)
        {
          byte[] data = ((ByteArrayBlob) blob).getBytes();
          byteBuf.writeBytes(data);
          ctx.write(byteBuf, promise);
        }
        else if (blob instanceof FileBlob)
        {
          File file = ((FileBlob) blob).getFile();
          if (file.length() < MINIMUM_SIZE_FOR_ZERO_COPY_SEND)
          {
            byteBuf.writeBytes(new FileInputStream(file), (int)file.length());
            ctx.writeAndFlush(byteBuf, promise);
          }
          else
          {
            RandomAccessFile raf = new RandomAccessFile(((FileBlob) blob).getFile(), "r");
            FileRegion fr = new DefaultFileRegion(raf.getChannel(), 0, raf.length());
            ctx.write(byteBuf);
            ctx.writeAndFlush(fr, promise);
          }
        }
        log.debug("Done writing data");
      }
      else log.debug("No data to write");

      if ("close".equals(httpResponse.getHeaders().getOrDefault("Connection", ""))) {
        log.debug("Closing connection after write");
        promise.addListener((future) -> ctx.channel().close());
      }
    }
    else // whatever it is, pass it on...
    {
      ctx.write(msg, promise);
    }
  }


/*  @Override
  protected void encode(final ChannelHandlerContext channelHandlerContext,
      final HttpResponse httpResponse,
      final ByteBuf byteBuf) throws Exception
  {
  }*/
}
