package ch.fhnw.bacnetit.transportbinding.ws;

public interface EndPointHandler {
    IncomingConnectionHandler getServerChannel();
}
