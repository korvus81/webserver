package net.jeffpoole.httpserver.parsing;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.LineBasedFrameDecoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;


/**
 * User: jpoole Date: 9/12/15 Time: 10:54 AM
 */
@Slf4j
@Value
public class HttpRequest
{
  String method;
  String target;
  String httpVersion;
  Map<String,String> headers;


  // This is primarily for ease of testing
  public static HttpRequest parse(String s) throws IOException
  {
    return parse(CharSource.wrap(s).openStream());
  }

  public static HttpRequest parse(InputStream is) throws IOException
  {
    // Should parse messages as a superset of US-ASCII per RFC 7230 § 3
    return parse(new InputStreamReader(is, Charsets.US_ASCII));
  }

  public static HttpRequest parse(Reader reader) throws IOException
  {
    BufferedReader bufferedReader = new BufferedReader(reader);

    String[] requestLine = bufferedReader.readLine().split("\\s+", 3);
    if (requestLine.length < 3) throw new IOException("Invalid request-line");
    final Map<String, String> headers = CharStreams.readLines(bufferedReader,
        new HttpHeaderProcessor());

    return new HttpRequest(requestLine[0], requestLine[1], requestLine[2], headers);

  }
/*
  public static HttpRequest parse(ByteBuf buf)
  {
    int i = buf.indexOf(buf.readerIndex(), buf.writerIndex() - 1, (byte) '\r');
    if (i != -1 && buf.getByte(i+1) == (byte) '\n' )
    {
      String[] requestLine = ;
    }
  }

  private String getLine(ByteBuf buf)
  {
    LineBasedFrameDecoder
    int i = buf.indexOf(buf.readerIndex(), buf.writerIndex() - 1, (byte) '\r');
    if (i != -1 && buf.getByte(i+1) == (byte) '\n' )
    {
      if (i == buf.readerIndex()) {
        buf.skipBytes(2);
        return "";
      }
      int len = i - buf.readerIndex();
    }
  }
  */
}
