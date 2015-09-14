package net.jeffpoole.httpserver.logic;

import static java.time.temporal.ChronoField.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.jeffpoole.httpserver.data.DataResource;
import net.jeffpoole.httpserver.data.DataSource;
import net.jeffpoole.httpserver.parsing.HttpRequest;
import net.jeffpoole.httpserver.parsing.HttpResponse;


/**
 * User: jpoole Date: 9/12/15 Time: 4:13 PM
 */
@Slf4j
@RequiredArgsConstructor
public class HttpServer
{
  final DataSource dataSource;

  // Based on DateTimeFormatter.RFC_1123_DATE_TIME, but with a 2-digit day, no TZ offset, and a more
  // rigid structure since the format has been fixed since 1995, and interpreting the legacy formats
  // would probably require different formatters anyway.
  // Format specified by http://tools.ietf.org/html/rfc7231#section-7.1.1.1
  private static final DateTimeFormatter RFC_5322_DATE_TIME;

  static
  {
    // manually code maps to ensure correct data always used
    // (locale data can be changed by application code)
    Map<Long, String> dow = new HashMap<>();
    dow.put(1L, "Mon");
    dow.put(2L, "Tue");
    dow.put(3L, "Wed");
    dow.put(4L, "Thu");
    dow.put(5L, "Fri");
    dow.put(6L, "Sat");
    dow.put(7L, "Sun");
    Map<Long, String> moy = new HashMap<>();
    moy.put(1L, "Jan");
    moy.put(2L, "Feb");
    moy.put(3L, "Mar");
    moy.put(4L, "Apr");
    moy.put(5L, "May");
    moy.put(6L, "Jun");
    moy.put(7L, "Jul");
    moy.put(8L, "Aug");
    moy.put(9L, "Sep");
    moy.put(10L, "Oct");
    moy.put(11L, "Nov");
    moy.put(12L, "Dec");
    RFC_5322_DATE_TIME = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .parseLenient()
        .appendText(DAY_OF_WEEK, dow)
        .appendLiteral(", ")
        .appendValue(DAY_OF_MONTH, 2)
        .appendLiteral(' ')
        .appendText(MONTH_OF_YEAR, moy)
        .appendLiteral(' ')
        .appendValue(YEAR, 4)  // 2 digit year not handled
        .appendLiteral(' ')
        .appendValue(HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(SECOND_OF_MINUTE, 2)
        .appendLiteral(" GMT")
        .toFormatter();
  }

  // Support only the required (per RFC 7231 § 4.1) methods for now
  final static Set<String> ALLOWED_METHODS = Sets.newHashSet("GET", "HEAD");


  public HttpResponse respond(HttpRequest req)
  {
    // Reject methods we do not allow
    if (!ALLOWED_METHODS.contains(req.getMethod()))
    {
      return HttpResponse.badRequest(Maps.newHashMap(), req);
    }

    String target = req.getTarget();
    log.debug("Method: [{}]  Target: [{}]", req.getMethod(), target);
    try
    {
      final DataResource dataResource = dataSource.get(target);

      if (!dataResource.isPresent())
      {
        return HttpResponse.notFound(Maps.newHashMap(), req);
      }
      Map<String, String> headers = Maps.newHashMap();
      if (dataResource.getContentType() != null)
      {
        headers.put("Content-Type", dataResource.getContentType());
      }
      if (dataResource.getEtag() != null)
      {
        headers.put("ETag", dataResource.getEtag());
      }
      if (dataResource.getModifiedTimestamp() != null)
      {
        headers.put("Last-Modified", instantToHttpDate(dataResource.getModifiedTimestamp()));
      }
      // MUST NOT send this if Transfer-Encoding is specified, per https://tools.ietf
      // .org/html/rfc7230#section-3.3.2
      if (dataResource.getSize().isPresent())
      {
        headers.put("Content-Length", String.valueOf(dataResource.getSize().get()));
      }
      headers.put("Date", instantToHttpDate(Instant.now()));

      if ("HEAD".equals(req.getMethod()))
      {
        // return everything except the body.
        log.debug("Returning HEAD response");
        return HttpResponse.ok(dataResource.withDataStream(() -> null), headers, req);
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


  String instantToHttpDate(Instant inst)
  {
    return RFC_5322_DATE_TIME.format(inst.atZone(ZoneOffset.UTC));
  }

}
