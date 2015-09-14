package net.jeffpoole.httpserver.data;

import static org.junit.Assert.*;

import java.io.InputStreamReader;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;


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
    DataResource res = fds.get(
        "/net/jeffpoole/httpserver/data/FileDataSourceTest.java");
    assertTrue(res.isPresent());
    assertEquals("text/x-java-source", res.getContentType());

    res = fds.get("/does/not/exist");
    assertFalse(res.isPresent());

    res = fds.get("/net");
    assertTrue(res.isPresent());
    assertEquals("text/html", res.getContentType());
    String content =
        CharStreams.toString(new InputStreamReader(res.getDataStream().get(), Charsets.UTF_8));
    assertTrue(content.contains("jeffpoole/"));
  }
}
