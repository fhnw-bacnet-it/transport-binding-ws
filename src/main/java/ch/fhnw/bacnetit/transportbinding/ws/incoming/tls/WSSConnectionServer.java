package ch.fhnw.bacnetit.transportbinding.ws.incoming.tls;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

import ch.fhnw.bacnetit.ase.application.auth.X509AccessControl;
import ch.fhnw.bacnetit.ase.application.auth.http.HttpBasicAuthHandler;
import ch.fhnw.bacnetit.ase.application.auth.http.HttpCorsHandler;
import ch.fhnw.bacnetit.ase.application.configuration.api.HttpAuthConfig;
import ch.fhnw.bacnetit.ase.application.configuration.api.KeystoreConfig;
import ch.fhnw.bacnetit.ase.application.configuration.api.TruststoreConfig;
import ch.fhnw.bacnetit.ase.network.transport.ConnectionServer;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;

public class WSSConnectionServer implements ConnectionServer {
    private CorsConfig corsConfig = null;
    private HttpAuthConfig httpAuthConfig = null;
    private String secProtocol = null;
    protected final KeystoreConfig keystoreConfig;
    protected final TruststoreConfig truststoreConfig;

    public WSSConnectionServer(final KeystoreConfig keystoreConfig,
            final TruststoreConfig truststoreConfig) {
        this.keystoreConfig = keystoreConfig;
        this.truststoreConfig = truststoreConfig;

    }

    public void setCorsConfig(final CorsConfig config) {
        this.corsConfig = config;
    }

    public void setHttpAuthConfig(final HttpAuthConfig config) {
        this.httpAuthConfig = config;
    }

    public void setSecprotocol(final String secProtocol) {
        this.secProtocol = secProtocol;
    }

    // Initial Pipeline
    @Override
    public ChannelHandler[] getChannelHandlers() {

        final List<ChannelHandler> handlers = new LinkedList<ChannelHandler>();
        SSLContext sslCtxServer = null;
        KeyStore ks;

        try (final InputStream readStream = new FileInputStream(
                keystoreConfig.path)) {
            // Keystore Server
            ks = KeyStore.getInstance("JKS");
            ks.load(readStream, keystoreConfig.pass.toCharArray());

            final KeyManagerFactory kmf = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keystoreConfig.pass.toCharArray());

            sslCtxServer = SSLContext.getInstance("TLS");
            sslCtxServer.init(kmf.getKeyManagers(), new TrustManager[] {
                    new X509AccessControl(keystoreConfig, truststoreConfig) },
                    null);

            final SSLEngine engine = sslCtxServer.createSSLEngine();
            engine.setUseClientMode(false);
            engine.setNeedClientAuth(true);
            handlers.add(new SslHandler(engine));

            handlers.add(new HttpServerCodec());
            handlers.add(new HttpObjectAggregator(8192));
            // Allowing CORS
            if (corsConfig != null) {
                // Building CORS Config
                // CorsConfig corsConfig =
                // CorsConfigBuilder.forAnyOrigin().allowCredentials()
                // .allowedRequestHeaders("Authorization", "Vary", "Cookie",
                // "Content-Type", "Accept", "X-Requested-With")
                // .allowedRequestMethods(HttpMethod.GET,
                // HttpMethod.OPTIONS).build();
                handlers.add(new HttpCorsHandler(corsConfig));
            }
            // Allow Http Authx
            if (httpAuthConfig != null) {
                handlers.add(new HttpBasicAuthHandler(httpAuthConfig));
            }

            // TODO must be easier to construct a connection -> through factory
            // keystore configs are passed all the way from WSConnectionFactory
            // pass a factory or initiate one here
            final WSSConnectionServerHandler serverHandler = new WSSConnectionServerHandler(
                    keystoreConfig, truststoreConfig);
            serverHandler.setSecProtocol(secProtocol);
            handlers.add(serverHandler);
            handlers.add(new LoggingHandler(LogLevel.DEBUG));

        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.err.println(e);
        }
        return handlers.toArray(new ChannelHandler[handlers.size()]);
    }

}
