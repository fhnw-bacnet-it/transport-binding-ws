package ch.fhnw.bacnetit.binding.ws.outgoing.tls;

import java.net.InetSocketAddress;

import ch.fhnw.bacnetit.stack.application.configuration.KeystoreConfig;
import ch.fhnw.bacnetit.stack.application.configuration.TruststoreConfig;
import ch.fhnw.bacnetit.stack.network.transport.ConnectionClient;
import ch.fhnw.bacnetit.stack.network.transport.ConnectionClientPipe;

public class WSSConnectionClientFactory implements ConnectionClientPipe {

    private final KeystoreConfig keystoreConfig;
    private final TruststoreConfig truststoreConfig;
    protected String secprotocol = null;

    public WSSConnectionClientFactory(final KeystoreConfig keystoreConfig,
            final TruststoreConfig truststoreConfig) {
        this.keystoreConfig = keystoreConfig;
        this.truststoreConfig = truststoreConfig;
    }

    public void setSecprotocol(final String secprotocol) {
        this.secprotocol = secprotocol;
    }

    @Override
    public ConnectionClient provideConnectionClient(
            final InetSocketAddress remoteAddress) {
        return new WSSConnection(remoteAddress, secprotocol, keystoreConfig,
                truststoreConfig);
    }

}
