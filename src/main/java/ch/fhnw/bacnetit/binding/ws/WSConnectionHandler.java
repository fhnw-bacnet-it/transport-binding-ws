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
package ch.fhnw.bacnetit.binding.ws;

import ch.fhnw.bacnetit.stack.encoding.ControlMessageInitEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class WSConnectionHandler extends SimpleChannelInboundHandler<Object> {
    private static final InternalLogger LOG = InternalLoggerFactory
            .getInstance(WSConnectionHandler.class);

    private final WebSocketClientHandshaker handshaker;

    private ChannelPromise handshakeFuture;

    public WSConnectionHandler(final WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        ctx.pipeline().remove(WSEncoder.class);
        LOG.debug("WebSocket Client disconnected!");
    }

    // Clientside pipeline changes after WebSocket Upgrade
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final Object msg)
            throws Exception {
        final Channel ch = ctx.channel();
        if (msg instanceof FullHttpResponse) {
            final FullHttpResponse response = (FullHttpResponse) msg;
            if (handshaker.isHandshakeComplete()) {
                throw new IllegalStateException(
                        "Unexpected FullHttpResponse with status "
                                + response.status() + " and content " + response
                                        .content().toString(CharsetUtil.UTF_8));
            } else {
                handshaker.finishHandshake(ch, response);
                LOG.debug("WebSocket Client connected!");
                handshakeFuture.setSuccess();

                ctx.pipeline().addBefore(
                        WSConnectionHandler.class.getSimpleName(),
                        ControlMessageHandler.class.getSimpleName(),
                        new ControlMessageHandler());

                ctx.pipeline().addBefore(
                        WSConnectionHandler.class.getSimpleName(),
                        WSBinaryFrameHandler.class.getSimpleName(),
                        new WSBinaryFrameHandler());

                ctx.pipeline().addBefore(
                        WSConnectionHandler.class.getSimpleName(),
                        WSEncoder.class.getSimpleName(), new WSEncoder());

                ctx.pipeline().remove(WSConnectionHandler.class);
                // UserEvent to notify the WebSocket Handshake Completeness
                LOG.debug("WebSocketConnectionHandler:");
                LOG.debug(
                        "Fire User Event Trigger, dummy Control Message object");
                ctx.pipeline()
                        .fireUserEventTriggered(new ControlMessageInitEvent());
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
            final Throwable cause) {
        ctx.fireExceptionCaught(cause);
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
}
