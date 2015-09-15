package net.jeffpoole.httpserver.datasource;

import java.time.Instant;
import java.util.Optional;

import lombok.Value;
import lombok.experimental.Wither;

import net.jeffpoole.httpserver.data.Blob;


/**
 * This represents some resource.  It can have etags, modification dates, content-type, etc.
 * The Blob component represents the data -- an abstraction was needed so we could handle both raw
 * (array of bytes) data or data from a file (which we can use zero-copy transfers for).
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
  Blob data;

  // For use when we don't have data to reference
  public static DataResource NO_DATA =
      new DataResource("", false, null, null, null, Optional.empty(), null);
}
