package ch.fhnw.bacnetit.transportbinding.ws;

import ch.fhnw.bacnetit.ase.encoding.ControlMessage;
import ch.fhnw.bacnetit.ase.encoding.ControlMessageReceivedEvent;
import ch.fhnw.bacnetit.ase.encoding._ByteQueue;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public class ControlMessageHandler extends SimpleChannelInboundHandler<Object> {
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg)
            throws Exception {
        final ByteBuf buffer = ((BinaryWebSocketFrame) msg).content();
        // Skip version
        buffer.skipBytes(1);
        final byte[] bytes = new byte[buffer.readableBytes()];

        buffer.readBytes(bytes);
        buffer.resetReaderIndex();

        final _ByteQueue queue = new _ByteQueue(bytes);
        // Check if ControlMessage
        if ((queue.peek(0) >> 6) == (byte) 1 && (queue.peek(1) == ((byte) 1)
                || queue.peek(2) == ((byte) 2))) {
            final ControlMessage cm = new ControlMessage(queue);
            final ControlMessageReceivedEvent cmre = new ControlMessageReceivedEvent(
                    cm);
            ctx.fireUserEventTriggered(cmre);

        }
        // or normal TBAPDU
        else if ((queue.peek(0) >> 6) == (byte) 0) {
            ctx.fireChannelRead(msg);
        }

    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx,
            final Object msg) throws Exception {
        // TODO Auto-generated method stub

    }

}
