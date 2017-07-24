package ch.fhnw.bacnetit.transportbinding.ws;

public interface ConnectionServerPipe {
    public ConnectionServer createConnectionServer();

    public int getServerPort();
}
