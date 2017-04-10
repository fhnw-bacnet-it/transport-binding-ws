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

import ch.fhnw.bacnetit.stack.encoding.ControlMessage;
import ch.fhnw.bacnetit.stack.encoding.NetworkPriority;
import ch.fhnw.bacnetit.stack.encoding.TPDU;
import ch.fhnw.bacnetit.stack.encoding._ByteQueue;
import ch.fhnw.bacnetit.stack.encoding.UnsignedInteger31;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class WSEncoder extends ChannelOutboundHandlerAdapter {
    private static final InternalLogger LOG = InternalLoggerFactory
            .getInstance(WSEncoder.class);

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg,
            final ChannelPromise promise) {
        ByteBuf content = null;
        // Structure of byte stream depends on chosen binding

        if (msg instanceof TPDU) {
            final _ByteQueue queue = new _ByteQueue();

            // Adding the WebSocketPayload Version Byte
            final byte versionByte = WSPayloadControl.getVersionByte();
            queue.push(versionByte);

            // Expecting result or not
            final WSPayloadControl.ExpectingReply expectingResult = ((TPDU) msg)
                    .isConfirmedRequest()
                            ? WSPayloadControl.ExpectingReply.EXPECTING
                            : WSPayloadControl.ExpectingReply.NOTEXPECTING;
            // Adding the WebSocketPayload Control Byte
            final byte controlByte = WSPayloadControl.getControlByte(
                    WSPayloadControl.PayloadType.TBPDU, expectingResult,
                    ((TPDU) msg).getPriority());
            queue.push(controlByte);

            // Adding the APDU
            ((TPDU) msg).write(queue);
            final byte[] inboundBuffer = queue.popAll();

            content = Unpooled.buffer();
            content.writeBytes(inboundBuffer);
        } else if (msg instanceof ByteBuf) {
            content = (ByteBuf) msg; // TODO is this even used anymore??
        } else if (msg instanceof ControlMessage) {

            final _ByteQueue queue = new _ByteQueue();
            // Adding the WebSocketPayload Version Byte
            final byte versionByte = WSPayloadControl.getVersionByte();
            queue.push(versionByte);

            // Expecting result or not
            final WSPayloadControl.ExpectingReply expectingResult = WSPayloadControl.ExpectingReply.NOTEXPECTING;
            // Adding the WebSocketPayload Control Byte
            final byte controlByte = WSPayloadControl.getControlByte(
                    WSPayloadControl.PayloadType.CONTROLMESSAGE,
                    expectingResult,
                    new UnsignedInteger31(NetworkPriority.NORMAL));
            queue.push(controlByte);

            // Adding ControlMessage to ByteQueue
            ((ControlMessage) msg).write(queue);
            final byte[] inboundBuffer = queue.popAll();
            content = Unpooled.buffer();
            content.writeBytes(inboundBuffer);

        }
        if (content != null) {
            // Prepare the websocket binary request.
            final WebSocketFrame frame = new BinaryWebSocketFrame(content);

            LOG.debug("Send websocket binary frame");

            final ChannelFuture cf = ctx.write(frame);

            cf.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture future) {
                    if (future.isSuccess()) {
                        // notify that the operation was successful

                        future.channel().pipeline().fireUserEventTriggered(msg);
                        LOG.debug(
                                "WebSocket Request/Response to BACnet host was successful.");
                    } else {
                        final Throwable cause = future.cause();

                        ctx.fireExceptionCaught(
                                new Exception(cause.getMessage(), null));
                        promise.setFailure(cause);
                        future.channel().close();
                    }
                }
            });
        } else {
            ctx.fireExceptionCaught(new Exception(
                    "No content or no remote address are set", null));
        }
    }

}
