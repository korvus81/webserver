package net.jeffpoole.httpserver.logic;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.jeffpoole.httpserver.datasource.DataResource;
import net.jeffpoole.httpserver.datasource.DataSource;
import net.jeffpoole.httpserver.parsing.HttpRequest;
import net.jeffpoole.httpserver.parsing.HttpResponse;


/**
 * This class has one method that takes a HttpRequest object, attempts to retrieve the resource
 * referenced using it's local DataSource, and returns a HttpResponse object.
 */

@Slf4j
@RequiredArgsConstructor
public class HttpServer
{
  final DataSource dataSource;

  // Support only the required (per RFC 7231 § 4.1) methods for now
  final static Set<String> ALLOWED_METHODS = Sets.newHashSet("GET", "HEAD");


  public HttpResponse respond(HttpRequest req)
  {
    // Reject methods we do not allow
    if (!ALLOWED_METHODS.contains(req.getMethod()))
      return HttpResponse.badRequest(Maps.newHashMap(), req);

    String target = req.getTarget();
    log.debug("Method: [{}]  Target: [{}]", req.getMethod(), target);
    try
    {
      final DataResource dataResource = dataSource.get(target);
      Map<String, String> headers = Maps.newHashMap();

      // If the target was not found, return our 404
      if (!dataResource.isPresent())
        return HttpResponse.notFound(headers, req);

      // Add headers about the target if we have the information
      if (dataResource.getContentType() != null)
        headers.put("Content-Type", dataResource.getContentType());

      if (dataResource.getEtag() != null)
        headers.put("ETag", dataResource.getEtag());

      if (dataResource.getModifiedTimestamp() != null)
        headers.put("Last-Modified", HttpServerUtils.instantToHttpDate(dataResource.getModifiedTimestamp()));

      // MUST NOT send this if Transfer-Encoding is specified, per
      // https://tools.ietf.org/html/rfc7230#section-3.3.2
      if (dataResource.getSize().isPresent())
        headers.put("Content-Length", String.valueOf(dataResource.getSize().get()));

      // Always include the date served, for caching reasons
      headers.put("Date", HttpServerUtils.instantToHttpDate(Instant.now()));

      // Either return the full response (for GET) or just the headers (for HEAD)
      if ("HEAD".equals(req.getMethod()))
      {
        // return everything except the body.
        log.debug("Returning HEAD response");
        return HttpResponse.ok(dataResource.withData(null), headers, req);
      }
      else // GET
      {
        log.debug("Returning GET response");
        return HttpResponse.ok(dataResource, headers, req);
      }
    }
    catch (Exception e)
    {
      log.error("Error getting datasource for target [{}]", target, e);
      return HttpResponse.badRequest(Maps.newHashMap(), req);
    }
  }



}
