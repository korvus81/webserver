package net.jeffpoole.httpserver.logic;

import static java.time.temporal.ChronoField.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;


/**
 * Right now, this just contains a utility function to produce proper HTTP dates, per RFC 7231
 */
public class HttpServerUtils
{
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


  public static String instantToHttpDate(final Instant inst)
  {
    return RFC_5322_DATE_TIME.format(inst.atZone(ZoneOffset.UTC));
  }
}
