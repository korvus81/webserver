package net.jeffpoole.httpserver.datasource;

/**
 * Interface for something that returns a resource given a path.  Only implementation outside of
 * test classes is FileDataSource.
 */
public interface DataSource
{
  DataResource get(String path);
}
