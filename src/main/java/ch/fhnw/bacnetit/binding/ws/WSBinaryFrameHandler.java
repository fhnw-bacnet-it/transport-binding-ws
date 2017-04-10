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

import java.util.Arrays;

import ch.fhnw.bacnetit.stack.encoding.TPDU;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Decodes a binary WebSocket frame and outputs a valid TBPDU. Sends a Reject if
 * the frame could not be decoded.
 */
public class WSBinaryFrameHandler
        extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {
    private static final InternalLogger LOG = InternalLoggerFactory
            .getInstance(WSBinaryFrameHandler.class);

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx,
            final BinaryWebSocketFrame msg) throws Exception {
        final ByteBuf buffer = msg.content();

        // Remove version and control byte
        buffer.skipBytes(2);

        final byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);

        // Translate here to BACnetMessage
        try {
            final TPDU bmsg = new TPDU(bytes);
            System.out.println("does a invoke id exists no3?:");
            System.out.println(bmsg.getInvokeId());
            LOG.debug("Successfully decoded new BACnetMessage: " + bmsg);
            ctx.fireChannelRead(bmsg);
        } catch (final Exception e) { // TODO decide on exception to throw on
                                      // invalid
            // TBPDU
            LOG.debug("Failed to decode BACnetMessage: "
                    + Arrays.toString(bytes));
            // TODO decide on error message to throw to client
            // ctx.writeAndFlush(new BACnetTransactionManagerException(
            // BACnetErrorType.TOO_MANY_TRANSACTIONS_ERROR));
        }
    }

}
