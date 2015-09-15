package net.jeffpoole.httpserver.data;

import lombok.Value;


/**
 * A Blob used when data is in a byte array (used for directory listings, since they don't exist on
 * disk).
 */
@Value
public class ByteArrayBlob implements Blob
{
  byte[] bytes;
}
