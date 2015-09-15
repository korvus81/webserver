package net.jeffpoole.httpserver.parsing;

import java.util.Map;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import net.jeffpoole.httpserver.datasource.DataResource;


/**
 * This is a value class that represents a HTTP response, with several static constructors for common
 * responses.
 */

@Value
@Slf4j
public class HttpResponse
{
  String httpVersion;
  int statusCode;
  String statusReason;
  Map<String,String> headers;
  DataResource resource;

  public static HttpResponse ok(DataResource resource, Map<String,String> headers, HttpRequest req)
  {
    addDefaultHeaders(headers, req);
    return new HttpResponse("HTTP/1.1", 200, "OK", headers, resource);
  }

  public static HttpResponse notFound(Map<String,String> headers, HttpRequest req)
  {
    addDefaultHeaders(headers, req);
    return new HttpResponse("HTTP/1.1", 404, "Not Found", headers, DataResource.NO_DATA);
  }

  public static HttpResponse badRequest(Map<String,String> headers, HttpRequest req)
  {
    addDefaultHeaders(headers, req);
    return new HttpResponse("HTTP/1.1", 400, "Bad Request", headers, DataResource.NO_DATA);
  }

  public static HttpResponse notImplemented(Map<String,String> headers, HttpRequest req)
  {
    addDefaultHeaders(headers, req);
    return new HttpResponse("HTTP/1.1", 501, "Not Implemented", headers, DataResource.NO_DATA);
  }

  private static void addDefaultHeaders(Map<String,String> headers, HttpRequest req)
  {
    // This should only be the case for HEAD requests
    if (!headers.containsKey("Content-Length")) headers.put("Content-Length", "0");

    if (!headers.containsKey("Server")) headers.put("Server", "jeffpoole-adobe");

    // Everything below here is to determine what Connection header we should send
    // If the request had Connection: close, we want to send Connection: close (and close the
    // connection when we are done)
    if ("close".equalsIgnoreCase(req.getHeaders().getOrDefault("Connection", "")))
      headers.put("Connection","close");

    if ("HTTP/1.0".equals(req.getHttpVersion()))
    {
      if ("keep-alive".equalsIgnoreCase(req.getHeaders().getOrDefault("Connection", "")))
        headers.put("Connection", "Keep-Alive");
      else
        headers.put("Connection", "close");
    }
  }
}
