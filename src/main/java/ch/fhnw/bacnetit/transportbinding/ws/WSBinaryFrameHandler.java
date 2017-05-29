package ch.fhnw.bacnetit.transportbinding.ws;

import java.util.Arrays;

import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
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
