package ch.fhnw.bacnetit.transportbinding.ws;

import io.netty.channel.ChannelHandler;

public interface ConnectionServer {
    ChannelHandler[] getChannelHandlers();
}
