package net.jeffpoole.httpserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.jeffpoole.httpserver.datasource.FileDataSource;
import net.jeffpoole.httpserver.logic.HttpServer;
import net.jeffpoole.httpserver.netty.NettyHttpServerInboundDecoder;
import net.jeffpoole.httpserver.netty.NettyHttpServerInboundHandler;
import net.jeffpoole.httpserver.netty.NettyHttpServerOutboundEncoder;
import net.jeffpoole.httpserver.netty.NettyHttpServerOutboundHandler;


/**
 * This is the main entrypoint of the server.
 * The start() method sets up a Netty NioServerSocketChannel, listening on the provided port.
 * Incoming requests go through the pipeline as follows:
 *  NettyHttpServerInboundDecoder -> NettyHttpServerInboundHandler
 *  (which then passes
 * Outgoing requests just go through NettyHttpServerOutboundEncoder then straight to the network.
 *
 */
@RequiredArgsConstructor
@ToString
@Slf4j
public class NettyWebserverMain
{
  final int listenPort;
  final Path pathToServe;


  public void start() throws Exception
  {
    final HttpServer httpServer = new HttpServer(new FileDataSource(pathToServe));
    EventLoopGroup group = new NioEventLoopGroup();
    try
    {
      // TODO: support setting specific interfaces / addresses to listen on
      InetSocketAddress isa = new InetSocketAddress(listenPort);

      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(group)
          .channel(NioServerSocketChannel.class)
          .localAddress(isa)
          .childHandler(new ChannelInitializer<SocketChannel>()
          {
            @Override
            public void initChannel(SocketChannel ch)
            {
              ch.pipeline()
                  .addLast(new NettyHttpServerInboundDecoder())
                  .addLast(new NettyHttpServerInboundHandler(httpServer))
                  .addLast(new NettyHttpServerOutboundHandler());
            }
          });
      log.info("Binding to port [{}]", this.listenPort);

      // This will bind and wait for it to complete
      ChannelFuture channelFuture = bootstrap.bind().sync();

      // This blocks on the channel getting closed
      channelFuture.channel().closeFuture().sync();
    }
    finally
    {
      group.shutdownGracefully().sync();
    }

  }

  public static void setLoggingLevel(Level level) {
    Logger root = (Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(level);
  }


  public static void main(String[] args) throws Exception
  {
    setLoggingLevel(Boolean.getBoolean("debug") ? Level.DEBUG : Level.WARN);



    // defaults
    int port = 8080;
    String path = "./wwwroot/";

    // If one argument, that is the port to run on, if two, the second is the path to serve.
    if (args.length >= 1)
    {
      port = Integer.parseInt(args[0]);
    }
    if (args.length >= 2)
    {
      path = args[1];
    }


    System.out.println(String.format("Server starting on port [%d] serving path [%s]...", port, path));

    NettyWebserverMain serverMain = new NettyWebserverMain(port, Paths.get(path));
    serverMain.start();
  }
}
