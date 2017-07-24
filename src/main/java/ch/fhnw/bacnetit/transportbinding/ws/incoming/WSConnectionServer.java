package ch.fhnw.bacnetit.transportbinding.ws.incoming;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.bacnetit.ase.application.configuration.api.HttpAuthConfig;
import ch.fhnw.bacnetit.transportbinding.auth.http.HttpBasicAuthHandler;
import ch.fhnw.bacnetit.transportbinding.auth.http.HttpCorsHandler;
import ch.fhnw.bacnetit.transportbinding.ws.ConnectionServer;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class WSConnectionServer implements ConnectionServer {
    private CorsConfig corsConfig = null;
    private HttpAuthConfig httpAuthConfig = null;
    private String secProtocol = null;

    public WSConnectionServer() {

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
        final List<ChannelHandler> handlers = new ArrayList<ChannelHandler>();

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
        // Allow Http Auth
        if (httpAuthConfig != null) {
            handlers.add(new HttpBasicAuthHandler(httpAuthConfig));
        }

        // TODO must be easier to construct a connection -> through factory
        // keystore configs are passed all the way from WSConnectionFactory
        // pass a factory or initiate one here
        final WSConnectionServerHandler serverHandler = new WSConnectionServerHandler();
        serverHandler.setSecProtocol(secProtocol);
        handlers.add(serverHandler);
        handlers.add(new LoggingHandler(LogLevel.DEBUG));
        return handlers.toArray(new ChannelHandler[handlers.size()]);
    }

}
