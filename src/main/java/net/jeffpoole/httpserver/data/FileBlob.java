package net.jeffpoole.httpserver.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;


/**
 * User: jpoole Date: 9/14/15 Time: 12:54 PM
 */
@Value
@Slf4j
public class FileBlob implements Blob
{
  File file;


  @Override
  public byte[] getBytes()
  {
    try
    {
      return Files.readAllBytes(file.toPath());
    }
    catch (IOException e)
    {
      log.error("Error reading bytes from file", e);
      return null;
    }
  }
}
