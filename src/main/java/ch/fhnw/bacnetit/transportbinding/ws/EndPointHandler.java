package ch.fhnw.bacnetit.transportbinding.ws;

import ch.fhnw.bacnetit.transportbinding.IncomingConnectionHandler;

public interface EndPointHandler {
    IncomingConnectionHandler getServerChannel();
}
