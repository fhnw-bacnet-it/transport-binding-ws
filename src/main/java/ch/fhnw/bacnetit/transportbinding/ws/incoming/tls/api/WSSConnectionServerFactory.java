package ch.fhnw.bacnetit.transportbinding.ws.incoming.tls.api;

import ch.fhnw.bacnetit.ase.application.configuration.api.KeystoreConfig;
import ch.fhnw.bacnetit.ase.application.configuration.api.TruststoreConfig;
import ch.fhnw.bacnetit.transportbinding.ws.ConnectionServer;
import ch.fhnw.bacnetit.transportbinding.ws.ConnectionServerPipe;
import ch.fhnw.bacnetit.transportbinding.ws.incoming.tls.WSSConnectionServer;

public class WSSConnectionServerFactory implements ConnectionServerPipe {
    protected final int port;
    protected final KeystoreConfig keystoreConfig;
    protected final TruststoreConfig truststoreConfig;

    public WSSConnectionServerFactory(final int serverPort,
            final KeystoreConfig keystoreConfig,
            final TruststoreConfig truststoreConfig) {
        this.keystoreConfig = keystoreConfig;
        this.truststoreConfig = truststoreConfig;
        this.port = serverPort;
    }

    @Override
    public ConnectionServer createConnectionServer() {
        return new WSSConnectionServer(keystoreConfig, truststoreConfig);
    }

    @Override
    public int getServerPort() {
        return port;
    }

}
