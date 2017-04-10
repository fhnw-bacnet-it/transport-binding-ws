/*******************************************************************************
 * Copyright (C) 2016 The Java BACnetITB Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ch.fhnw.bacnetit.binding.ws.incoming;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.bacnetit.stack.application.auth.http.HttpBasicAuthHandler;
import ch.fhnw.bacnetit.stack.application.auth.http.HttpCorsHandler;
import ch.fhnw.bacnetit.stack.application.configuration.HttpAuthConfig;
import ch.fhnw.bacnetit.stack.network.transport.ConnectionServer;
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
