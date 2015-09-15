package net.jeffpoole.httpserver.logic;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.Test;


/**
 * User: jpoole Date: 9/14/15 Time: 6:16 PM
 */
public class HttpServerUtilsTest
{

  @Test
  public void testInstantToHttpDate() throws Exception
  {
    assertEquals(
        "Sun, 06 Sep 2015 10:15:30 GMT",
        HttpServerUtils.instantToHttpDate(Instant.parse("2015-09-06T10:15:30.00Z")));
  }
}
