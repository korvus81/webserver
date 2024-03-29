package net.jeffpoole.httpserver.logic;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;

import net.jeffpoole.httpserver.data.ByteArrayBlob;
import net.jeffpoole.httpserver.datasource.DataResource;
import net.jeffpoole.httpserver.datasource.DataSource;
import net.jeffpoole.httpserver.parsing.HttpRequest;
import net.jeffpoole.httpserver.parsing.HttpResponse;


/**
 * User: jpoole Date: 9/13/15 Time: 10:38 AM
 */
@Slf4j
public class HttpServerTest
{

  HttpServer server;
  DataSource dataSource;

  static final String REQUEST_GET_TXT = "GET /file.txt HTTP/1.1\r\n" +
      "User-Agent: curl/7.16.3 libcurl/7.16.3 OpenSSL/0.9.7l zlib/1.2.3\r\n" +
      "Host: www.example.com\r\n" +
      "Accept-Language: en, mi\r\n" +
      "\r\n";

  static final String REQUEST_GET_NOT_FOUND = "GET /notfound HTTP/1.1\r\n" +
      "User-Agent: curl/7.16.3 libcurl/7.16.3 OpenSSL/0.9.7l zlib/1.2.3\r\n" +
      "Host: www.example.com\r\n" +
      "Accept-Language: en, mi\r\n" +
      "\r\n";

  static final String REQUEST_HEAD_TXT = "HEAD /file.txt HTTP/1.1\r\n" +
      "User-Agent: curl/7.16.3 libcurl/7.16.3 OpenSSL/0.9.7l zlib/1.2.3\r\n" +
      "Host: www.example.com\r\n" +
      "Accept-Language: en, mi\r\n" +
      "\r\n";

  static final String REQUEST_HEAD_NOT_FOUND = "HEAD /notfound HTTP/1.1\r\n" +
      "User-Agent: curl/7.16.3 libcurl/7.16.3 OpenSSL/0.9.7l zlib/1.2.3\r\n" +
      "Host: www.example.com\r\n" +
      "Accept-Language: en, mi\r\n" +
      "\r\n";


  @Before
  public void setUp() throws Exception
  {
    dataSource = mock(DataSource.class);
    when(dataSource.get("/file.txt")).thenReturn(new DataResource(
        "/file.txt",
        true,
        "abc",
        Instant.now(),
        "text/plain",
        Optional.of(4L),
        new ByteArrayBlob("abc\n".getBytes(Charsets.UTF_8))
    ));
    when(dataSource.get("/notfound")).thenReturn(new DataResource(
        "/notfound",
        false,
        null,
        null,
        null,
        Optional.<Long> empty(),
        null
    ));
    server = new HttpServer(dataSource);
  }


  @Test
  public void testRespondOk() throws Exception
  {
    HttpResponse txt = server.respond(HttpRequest.parse(REQUEST_GET_TXT));
    assertEquals(200, txt.getStatusCode());
    assertEquals("text/plain", txt.getHeaders().get("Content-Type"));
    String data = new String(((ByteArrayBlob)txt.getResource().getData()).getBytes(), Charsets.UTF_8);
    assertEquals("abc\n", data);
  }


  @Test
  public void testRespondNotFound() throws Exception
  {
    HttpResponse notfound = server.respond(HttpRequest.parse(REQUEST_GET_NOT_FOUND));
    assertEquals(404, notfound.getStatusCode());
  }

  @Test
  public void testRespondHead() throws Exception
  {
    HttpResponse txt = server.respond(HttpRequest.parse(REQUEST_HEAD_TXT));
    HttpResponse notfound = server.respond(HttpRequest.parse(REQUEST_HEAD_NOT_FOUND));
    assertEquals(200, txt.getStatusCode());
    assertEquals(404, notfound.getStatusCode());
    assertNull(txt.getResource().getData());
    assertNull(notfound.getResource().getData());
  }


}
