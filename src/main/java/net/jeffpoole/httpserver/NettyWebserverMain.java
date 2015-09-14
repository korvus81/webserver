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

import net.jeffpoole.httpserver.data.FileDataSource;
import net.jeffpoole.httpserver.logic.HttpServer;
import net.jeffpoole.httpserver.netty.NettyHttpServerInboundDecoder;
import net.jeffpoole.httpserver.netty.NettyHttpServerInboundHandler;
import net.jeffpoole.httpserver.netty.NettyHttpServerOutboundEncoder;


/**
 * Hello world!
 */
@RequiredArgsConstructor
@ToString
@Slf4j
public class NettyWebserverMain
{
  final int listenPort;
  final Path pathToServe;

  boolean running = true;

  final int cores = Runtime.getRuntime().availableProcessors();
  ExecutorService executorService = Executors.newFixedThreadPool(cores);


  public void start() throws Exception
  {
    final HttpServer httpServer = new HttpServer(new FileDataSource(pathToServe));
    EventLoopGroup group = new NioEventLoopGroup();
    try
    {
      InetAddress localhost = InetAddress.getLocalHost();
      InetSocketAddress isa = new InetSocketAddress(/*localhost, */listenPort);

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
                  .addLast(new NettyHttpServerOutboundEncoder())
                  //.addLast(new NettyHttpServerOutboundHandler())
              ;
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


  public static void main(String[] args) throws Exception
  {
    log.info("Server started");
    int port = 8080;
    if (args.length == 1)
    {
      port = Integer.parseInt(args[0]);
    }
    NettyWebserverMain serverMain = new NettyWebserverMain(port, Paths.get("/srv/www/"));
    serverMain.start();
  }
}
