package net.jeffpoole.httpserver.datasource;

/**
 * User: jpoole Date: 9/12/15 Time: 12:14 PM
 */
public interface DataSource
{
  DataResource get(String path);
}
