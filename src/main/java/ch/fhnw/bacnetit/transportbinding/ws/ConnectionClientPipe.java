package ch.fhnw.bacnetit.transportbinding.ws;

import java.net.InetSocketAddress;

public interface ConnectionClientPipe {
    public ConnectionClient provideConnectionClient(
            InetSocketAddress remoteAddress);
}
