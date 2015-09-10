package net.jeffpoole;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


/**
 * Hello world!
 */
@RequiredArgsConstructor
@ToString
@Slf4j
public class WebserverMain
{
  final int port;
  final Path pathToServe;

  public void init() throws Exception
  {
    // Create a new server socket and set to non blocking mode
    ServerSocketChannel ssc = ServerSocketChannel.open();
    ssc.configureBlocking(false);

    InetAddress lh = InetAddress.getLocalHost();
    InetSocketAddress isa = new InetSocketAddress(lh, port);
    ssc.socket().bind(isa);
  }

  public static void main(String[] args)
  {
    log.info("Server started");
  }
}
