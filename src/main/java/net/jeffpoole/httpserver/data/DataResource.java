package net.jeffpoole.httpserver.data;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import lombok.Value;
import lombok.experimental.Wither;


/**
 * User: jpoole Date: 9/12/15 Time: 12:23 PM
 */
@Value
@Wither
public class DataResource
{
  String path;
  boolean present;
  String etag;
  Instant modifiedTimestamp;
  String contentType;
  Optional<Long> size;
  // This is a supplier so we don't have to open an InputStream unless we actually need the data
  // This can return null
  Supplier<InputStream> dataStream;

  // For use when we don't have data to reference
  public static DataResource NO_DATA =
      new DataResource("", false, null, null, null, Optional.empty(), ()->null);
}
