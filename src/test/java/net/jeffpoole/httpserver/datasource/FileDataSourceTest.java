package net.jeffpoole.httpserver.datasource;

import static org.junit.Assert.*;

import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;

import net.jeffpoole.httpserver.data.ByteArrayBlob;
import net.jeffpoole.httpserver.datasource.DataResource;
import net.jeffpoole.httpserver.datasource.FileDataSource;


/**
 * User: jpoole Date: 9/13/15 Time: 3:50 PM
 */
@Slf4j
public class FileDataSourceTest
{

  FileDataSource fds;
  @Before
  public void setUp() throws Exception
  {
    fds = new FileDataSource(Paths.get("./src/test/java/"));
  }


  @Test
  public void testGet() throws Exception
  {
    // testing a real file
    DataResource res = fds.get("/net/jeffpoole/httpserver/datasource/FileDataSourceTest.java");
    assertTrue(res.isPresent());
    assertEquals("text/x-java-source", res.getContentType());

    // testing a 404
    res = fds.get("/does/not/exist");
    assertFalse(res.isPresent());

    // testing a directory listing
    res = fds.get("/net");
    assertTrue(res.isPresent());
    assertEquals("text/html", res.getContentType());
    String content = new String(((ByteArrayBlob)res.getData()).getBytes(), Charsets.UTF_8);
    assertTrue(content.contains("jeffpoole/"));
  }
}
