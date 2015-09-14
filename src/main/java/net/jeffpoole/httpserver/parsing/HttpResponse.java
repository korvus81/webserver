package net.jeffpoole.httpserver.parsing;

import java.util.Map;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import net.jeffpoole.httpserver.data.DataResource;


/**
 * User: jpoole Date: 9/12/15 Time: 10:54 AM
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
    if (!headers.containsKey("Content-Length")) headers.put("Content-Length","0");
    if (!headers.containsKey("Server")) headers.put("Server","jeffpoole");
    if ("HTTP/1.0".equals(req.getHttpVersion())) headers.put("Connection","close");
  }
}
