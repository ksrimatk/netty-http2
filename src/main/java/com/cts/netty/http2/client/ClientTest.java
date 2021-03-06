package com.cts.netty.http2.client;

import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cts.netty.http2.util.Http2Util;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.ssl.SslContext;

public class ClientTest {

    private static final Logger logger = LoggerFactory.getLogger(ClientTest.class);
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8443;
//    private 
    

    
	public static void main(String args[]) throws Exception {
		SslContext sslCtx = Http2Util.createSSLContext(false);
		Channel channel;
		 EventLoopGroup workerGroup = new NioEventLoopGroup();
	        Http2ClientInitializer initializer = new Http2ClientInitializer(sslCtx, Integer.MAX_VALUE, HOST, PORT);

	        try {
	            Bootstrap b = new Bootstrap();
	            b.group(workerGroup);
	            b.channel(NioSocketChannel.class);
	            b.option(ChannelOption.SO_KEEPALIVE, true);
	            b.remoteAddress(HOST, PORT);
	            b.handler(initializer);

	            channel = b.connect()
	                .syncUninterruptibly()
	                .channel();

	            System.out.println("Connected to [" + HOST + ':' + PORT + ']');

	            Http2SettingsHandler http2SettingsHandler = initializer.getSettingsHandler();
	            http2SettingsHandler.awaitSettings(60, TimeUnit.SECONDS);

	            logger.info("Sending request(s)...");

	            FullHttpRequest request = Http2Util.createGetRequest(HOST, PORT);

	            Http2ClientResponseHandler responseHandler = initializer.getResponseHandler();
	            int streamId = 3;

	            responseHandler.put(streamId, channel.write(request), channel.newPromise());
	            channel.flush();
	            String response = responseHandler.awaitResponses(60, TimeUnit.SECONDS);

	          System.out.println("Hello World"+ response);

	            logger.info("Finished HTTP/2 request(s)");

	        } finally {
	            workerGroup.shutdownGracefully();
	        }

	}
}
