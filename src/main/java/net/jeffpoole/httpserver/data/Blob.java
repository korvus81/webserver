package net.jeffpoole.httpserver.data;

/**
 * Abstraction for a source of data.
 */
public interface Blob
{
  byte[] getBytes();
}
