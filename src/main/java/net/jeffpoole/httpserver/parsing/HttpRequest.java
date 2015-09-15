package net.jeffpoole.httpserver.parsing;

import io.netty.buffer.ByteBuf;

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
 * This is a value class to hold a HTTP request.  As the current implementation only accepts GET and
 * HEAD, it doesn't support a request body since server behavior is undefined for requests with
 * bodies using those methods.
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

  public static HttpRequest parse(ByteBuf buf) throws IOException
  {
    String[] requestLine = getLine(buf).split("\\s+", 3);
    if (requestLine.length < 3) throw new RuntimeException("Invalid request-line");
    HttpHeaderProcessor hhp = new HttpHeaderProcessor();
    while (hhp.processLine(getLine(buf))) /* condition is the body */;
    return new HttpRequest(requestLine[0], requestLine[1], requestLine[2], hhp.getResult());
  }

  private static String getLine(ByteBuf buf)
  {
    final int eolPos = findEol(buf);
    if (eolPos > -1)
    {
      if (eolPos == buf.readerIndex()) {
        buf.skipBytes(2); // skip \r\n
        return "";
      }
      final byte[] bytes = new byte[eolPos-buf.readerIndex()];
      buf.readBytes(bytes);
      buf.skipBytes(2); // skip \r\n
      return new String(bytes, Charsets.UTF_8);
    }
    return "";
  }

  private static int findEol(final ByteBuf buf)
  {
    final int start = buf.readerIndex();
    final int end = buf.writerIndex()-1;
    for (int i = start; i<end; i++)
    {
      if (buf.getByte(i) == '\r' && buf.getByte(i+1) == '\n') return i;
    }
    return -1;
  }

}
