package com.cts.netty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cts.netty.http2.client.Http2ClientInitializer;
import com.cts.netty.http2.client.Http2ClientResponseHandler;
import com.cts.netty.http2.client.Http2SettingsHandler;
import com.cts.netty.http2.util.Http2Util;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.ssl.SslContext;

//Ensure the server class - Http2Server.java is already started before running this test
public class Http2ClientLiveTest {

    private static final Logger logger = LoggerFactory.getLogger(Http2ClientLiveTest.class);

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8443;
    private SslContext sslCtx;
    private Channel channel;

    @BeforeAll
    public void setup() throws Exception {
        sslCtx = Http2Util.createSSLContext(false);
    }

    @Test
    public void whenRequestSent_thenHelloWorldReceived() throws Exception {

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

            logger.info("Connected to [" + HOST + ':' + PORT + ']');

            Http2SettingsHandler http2SettingsHandler = initializer.getSettingsHandler();
            http2SettingsHandler.awaitSettings(60, TimeUnit.SECONDS);

            logger.info("Sending request(s)...");

            FullHttpRequest request = Http2Util.createGetRequest(HOST, PORT);

            Http2ClientResponseHandler responseHandler = initializer.getResponseHandler();
            int streamId = 3;

            responseHandler.put(streamId, channel.write(request), channel.newPromise());
            channel.flush();
            String response = responseHandler.awaitResponses(60, TimeUnit.SECONDS);

            assertEquals("Hello World", response);

            logger.info("Finished HTTP/2 request(s)");

        } finally {
            workerGroup.shutdownGracefully();
        }

    }

    @AfterAll
    public void cleanup() {
        channel.close()
            .syncUninterruptibly();
    }
}