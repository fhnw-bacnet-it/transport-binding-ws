/*******************************************************************************
 * Copyright (C) 2016 The Java BACnetITB Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ch.fhnw.bacnetit.binding.ws.outgoing;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ch.fhnw.bacnetit.binding.ws.WSConnectionHandler;
import ch.fhnw.bacnetit.stack.application.exception.StackInitializationException;
import ch.fhnw.bacnetit.stack.network.transport.StatefulConnectionClient;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class WSConnection implements StatefulConnectionClient {
    private static final InternalLogger LOG = InternalLoggerFactory
            .getInstance(WSConnection.class);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    protected WSConnectionHandler webSocketClientHandler;
    protected Channel channel;
    private final InetSocketAddress remoteAddress;

    public WSConnection(final InetSocketAddress remoteAddress,
            final String subprotocol) {
        this.remoteAddress = remoteAddress;
        try {
            final HttpHeaders httpHeaders = new DefaultHttpHeaders();
            final WebSocketClientHandshaker webSocketClientHandshaker = WebSocketClientHandshakerFactory
                    .newHandshaker(
                            new URI("ws://" + remoteAddress.getHostString()
                                    + ":" + remoteAddress.getPort()
                                    + "/websocket"),
                            WebSocketVersion.V13, subprotocol, false,
                            httpHeaders);
            webSocketClientHandler = new WSConnectionHandler(
                    webSocketClientHandshaker);
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        workerGroup.shutdownGracefully();
    }

    @Override
    public SocketAddress getAddress() {
        return remoteAddress;
    }

    @Override
    public URI getURI() {
        // TODO check correctness and need of method -> getURI is actually never
        // used
        try {
            return new URI("ws://" + remoteAddress.getHostName() + ":"
                    + remoteAddress.getPort());
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setChannel(final Channel channel) {
        this.channel = channel;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public ChannelHandler[] getChannelHandlers() {
        final List<ChannelHandler> handlers = new ArrayList<ChannelHandler>();
        handlers.add(new HttpClientCodec());
        handlers.add(new HttpObjectAggregator(8192));
        handlers.add(webSocketClientHandler);
        return handlers.toArray(new ChannelHandler[handlers.size()]);
    }

    @Override
    public void initialize() {
        try {
            System.out.println("initializing...");
            final ChannelFuture cf = webSocketClientHandler.handshakeFuture();
            System.out.println("adding listener...");
            cf.addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(final ChannelFuture future)
                        throws Exception {
                    if (!future.isSuccess()) {
                        LOG.error(
                                "Error during initialization of the websocket connection");
                        System.out.println(
                                "Error during initialization of the websocket connection");
                    } else {
                        LOG.info("Websocket connection initialized");
                        System.out.println("Websocket connection initialized");
                    }
                }
            });
            cf.await(1000, TimeUnit.MILLISECONDS);
            System.out.println("initialized");
        } catch (final InterruptedException e) {
            throw new StackInitializationException(e.getMessage(), null);
        }
    }

}
