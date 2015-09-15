package net.jeffpoole.httpserver.data;

import lombok.Value;


/**
 * User: jpoole Date: 9/14/15 Time: 12:54 PM
 */
@Value
public class ByteArrayBlob implements Blob
{
  byte[] bytes;
}
