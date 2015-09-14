package net.jeffpoole.httpserver.data;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.google.common.base.Charsets;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;


/**
 * User: jpoole Date: 9/12/15 Time: 12:14 PM
 */
@Slf4j
@RequiredArgsConstructor
public class FileDataSource implements DataSource
{
  final Path filesystemPath;
  Path canonicalFilesystemPath;
  Pattern relativePathPattern = Pattern.compile("[^/.].*$");


  public DataResource get(String requestPath)
  {
    if (canonicalFilesystemPath == null)
    {
      canonicalFilesystemPath = filesystemPath.toAbsolutePath();
    }

    // TODO: better checks make sure path does not escape base path
    if (requestPath.contains("/../"))
    {
      throw new RuntimeException("Invalid path");
    }

    try
    {
      // handle any urlencoding
      requestPath = URLDecoder.decode(requestPath, Charsets.UTF_8.toString());
      // remove query strings
      if (requestPath.contains("?"))
      {
        requestPath = requestPath.substring(0, requestPath.indexOf("?"));
      }
    }
    catch (UnsupportedEncodingException e)
    {
      log.warn("Problem urldecoding requestPath = [{}]", requestPath, e);
    }

    final Matcher matcher = relativePathPattern.matcher(requestPath);
    if (matcher.find())
    {
      String relativePath = matcher.group();
      Path resolved = filesystemPath.resolve(relativePath);
      if (isInside(canonicalFilesystemPath, resolved) && resolved.toFile().exists() &&
          resolved.toFile().canRead())
      {
        if (resolved.toFile().isDirectory() && resolved.resolve("index.html").toFile().canRead())
        {
          resolved = resolved.resolve("index.html");
        }
        final Path fresolved = resolved;
        if (resolved.toFile().isFile())
        {
          return new DataResource(
              resolved.toString(),
              true,
              calculateEtag(resolved),
              Instant.ofEpochMilli(resolved.toFile().lastModified()),
              determineContentType(resolved),
              Optional.of(resolved.toFile().length()),
              () -> {
                try
                {
                  return Files.newInputStream(fresolved, StandardOpenOption.READ);
                }
                catch (IOException e)
                {
                  return null;
                }
              }
          );
        }
        else if (resolved.toFile().isDirectory())
        {
          String directoryList = createDirectoryList(resolved, requestPath);
          byte[] directoryListBytes = directoryList.getBytes(Charsets.UTF_8);
          return new DataResource(
              resolved.toString(),
              true,
              Hashing.sha1().hashBytes(directoryListBytes).toString(),
              Instant.ofEpochMilli(resolved.toFile().lastModified()),
              "text/html",
              Optional.of((long) directoryListBytes.length),
              () -> {
                try
                {
                  return ByteSource.wrap(directoryListBytes).openStream();
                }
                catch (IOException e)
                {
                  return null;
                }
              }
          );
        }
      }
      else
      {
        log.warn("Path [{}] not inside path [{}], so returning not found", resolved,
            canonicalFilesystemPath);
      }
    }
    return new DataResource(requestPath, false, null, null, null, Optional.empty(), () -> null);
  }


  private String determineContentType(final Path resolved)
  {
    try (TikaInputStream tis = TikaInputStream.get(resolved.toFile()))
    {
      TikaConfig tikaConfig = new TikaConfig();
      Metadata metadata = new Metadata();
      metadata.set(Metadata.RESOURCE_NAME_KEY, resolved.toString());
      final MediaType detected = tikaConfig.getDetector().detect(tis, metadata);
      return detected.toString();
    }
    catch (Exception e)
    {
      log.warn("Error detecting proper content type, so returning our default", e);
    }
    return "text/plain";
  }


  boolean isInside(Path parent, Path child)
  {
    Path absoluteParent = parent.toAbsolutePath();
    Path absoluteChild = child.toAbsolutePath();
    try
    {
      while (!Files.isSameFile(absoluteChild, absoluteParent) && absoluteChild.getParent() != null)
      {
        absoluteChild = absoluteChild.getParent();
      }
      return Files.isSameFile(absoluteChild, absoluteParent);
    }
    catch (IOException e)
    {
      log.warn("Error checking that path [{}] is contained in [{}]", child, parent);
    }
    return false;
  }


  // Would love to base this on the contents of the file, but for performance I will use
  // name + modified time.  Similar to Apache's default implementation (which uses inode instead,
  // IIRC)
  String calculateEtag(Path path)
  {
    final Hasher hasher = Hashing.sha1().newHasher();
    return hasher
        .putInt(path.hashCode()) // based on the path
        .putLong(path.toFile().lastModified())
        .hash()
        .toString();
  }


  String createDirectoryList(Path path, String requestPath)
  {
    StringBuilder sb = new StringBuilder(
        "<!DOCTYPE html><meta charset=\"UTF-8\">"
            + "<html><head><title>Directory Listing for "
            + path.getFileName().toString()
            + "</title></head><body><ul>");
    String joinChr = "/";
    if (requestPath.endsWith("/"))
    {
      joinChr = "";
    }
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(path))
    {
      for (Path file : ds)
      {
        // skip unix-style hidden files
        if (file.getFileName().toString().startsWith("."))
        {
          continue;
        }
        String fileRequestPath = requestPath + joinChr + file.getFileName().toString();
        if (file.toFile().isDirectory())
        {
          fileRequestPath = fileRequestPath + "/";
        }
        sb.append(String.format("<li><a href=\"%s\">%s</a></li>", fileRequestPath,
            fileRequestPath));
      }
    }
    catch (IOException e)
    {
      log.error("Error getting directory listing", e);
      return "Error getting directory listing";
    }
    sb.append("</ul></body></html>");
    return sb.toString();
  }
}
