package net.jeffpoole.httpserver.parsing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.io.LineProcessor;


/**
 * This class builds up a map of headers, telling the caller to end processing when it finds an
 * empty line (indicative of a double CRLF, or the end of the headers).
 */
public class HttpHeaderProcessor implements LineProcessor<Map<String, String>>
{
  private Map<String,String> headers = new HashMap<String, String>();

  public boolean processLine(final String line) throws IOException
  {
    if (line.trim().length() == 0) return false;
    // No point supporting line continuation, since they are obsolete per RFC 7230 § 3.2.4
    if (line.startsWith(" ") || line.startsWith("\t"))
      throw new IOException("Header continuation lines not supported");

    final String[] parts = line.split(":", 2);

    if (parts.length < 2)
      throw new IOException("Invalid header line missing ':'");

    if (parts[0].endsWith(" ") || parts[0].endsWith("\t"))
      throw new IOException("Whitespace after header key not allowed");

    final String fieldName = parts[0].trim();
    String fieldValue = parts[1].trim();

    // We will support repeated headers as if they contain a comma-separated list per RFC 7230 § 3.2.2
    if (headers.containsKey(fieldName))
    {
      fieldValue = headers.get(fieldName) + "," + fieldValue;
    }
    headers.put(fieldName, fieldValue);
    return true;
  }


  public Map<String, String> getResult()
  {
    return headers;
  }
}
