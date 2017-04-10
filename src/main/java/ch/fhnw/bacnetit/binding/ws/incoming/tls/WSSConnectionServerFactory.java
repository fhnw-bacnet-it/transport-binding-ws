package ch.fhnw.bacnetit.binding.ws.incoming.tls;

import ch.fhnw.bacnetit.stack.application.configuration.KeystoreConfig;
import ch.fhnw.bacnetit.stack.application.configuration.TruststoreConfig;
import ch.fhnw.bacnetit.stack.network.transport.ConnectionServer;
import ch.fhnw.bacnetit.stack.network.transport.ConnectionServerPipe;

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
