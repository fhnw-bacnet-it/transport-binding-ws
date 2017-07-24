package ch.fhnw.bacnetit.transportbinding.ws.outgoing.tls.api;

import java.net.InetSocketAddress;

import ch.fhnw.bacnetit.ase.application.configuration.api.KeystoreConfig;
import ch.fhnw.bacnetit.ase.application.configuration.api.TruststoreConfig;
import ch.fhnw.bacnetit.transportbinding.ws.ConnectionClient;
import ch.fhnw.bacnetit.transportbinding.ws.ConnectionClientPipe;
import ch.fhnw.bacnetit.transportbinding.ws.outgoing.tls.WSSConnection;

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
