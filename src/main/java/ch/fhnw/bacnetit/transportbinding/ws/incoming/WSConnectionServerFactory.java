package ch.fhnw.bacnetit.transportbinding.ws.incoming;

import ch.fhnw.bacnetit.ase.network.transport.ConnectionServer;
import ch.fhnw.bacnetit.ase.network.transport.ConnectionServerPipe;

public class WSConnectionServerFactory implements ConnectionServerPipe {
    protected final int port;

    public WSConnectionServerFactory(final int serverPort) {
        this.port = serverPort;
    }

    @Override
    public ConnectionServer createConnectionServer() {
        return new WSConnectionServer();
    }

    @Override
    public int getServerPort() {
        return port;
    }

}
