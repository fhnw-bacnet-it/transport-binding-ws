package ch.fhnw.bacnetit.transportbinding.ws.outgoing;

import java.net.InetSocketAddress;

import ch.fhnw.bacnetit.ase.network.transport.ConnectionClient;
import ch.fhnw.bacnetit.ase.network.transport.ConnectionClientPipe;

public class WSConnectionClientFactory implements ConnectionClientPipe {

    protected String secprotocol = null;

    /**
     * Sets secprotocol for HTTP authentication
     *
     * @param secprotocol
     */
    public void setSecprotocol(final String secprotocol) {
        this.secprotocol = secprotocol;
    }

    /**
     * Constructs a WebSocket connection to a specified address. At this point
     * the connection is not initialized or bootstrapped.
     */
    @Override
    public ConnectionClient provideConnectionClient(
            final InetSocketAddress remoteAddress) {
        return new WSConnection(remoteAddress, secprotocol);
    }

}
