package net.jeffpoole.httpserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;

import com.google.common.base.Charsets;

import net.jeffpoole.httpserver.parsing.HttpRequest;


/**
 * This class is used to decode raw data coming in from the network into HttpRequest objects to be
 * handled by the next pipeline stage (NettyHttpServerInboundHandler).  Since we currently only
 * support GET and HEAD, no attempts are made to read a body.
 */
@Slf4j
public class NettyHttpServerInboundDecoder extends ByteToMessageDecoder
{

  @Override
  protected void decode(final ChannelHandlerContext channelHandlerContext, final ByteBuf byteBuf,
      final List<Object> list) throws Exception
  {
    int startIndex = byteBuf.readerIndex();
    while (startIndex < byteBuf.writerIndex())
    {
      int ind = byteBuf.indexOf(startIndex, byteBuf.writerIndex() - 3, (byte) '\r');
      if ((byteBuf.writerIndex() - ind) >= 4) // if there are at least 4 bytes to read...
      {
        String possibleEndOfHeaders = byteBuf.toString(ind, 4, Charsets.UTF_8);
        if (possibleEndOfHeaders.equals("\r\n\r\n"))
        {
          // double CRLF means we are at the end of the headers
          int len = (ind + 4) - byteBuf.readerIndex();
          list.add(HttpRequest.parse(byteBuf.readSlice(len)));
          startIndex = ind + 4;
        }
        else
        {
          startIndex = ind + 1;
        }
      }
      else
      {
        // at the end of the buffer and nothing is there to read
        startIndex = byteBuf.writerIndex();
      }
    }
  }


  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
      throws Exception
  {
    log.error("Inbound exception caught, closing channel", cause);
    ctx.close();
  }
}
