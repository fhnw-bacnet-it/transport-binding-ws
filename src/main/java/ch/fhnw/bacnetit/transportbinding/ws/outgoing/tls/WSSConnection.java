package ch.fhnw.bacnetit.transportbinding.ws.outgoing.tls;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import ch.fhnw.bacnetit.ase.application.ExceptionManager;
import ch.fhnw.bacnetit.ase.application.configuration.api.KeystoreConfig;
import ch.fhnw.bacnetit.ase.application.configuration.api.TruststoreConfig;
import ch.fhnw.bacnetit.ase.application.exception.StackInitializationException;
import ch.fhnw.bacnetit.ase.network.transport.StatefulConnectionClient;
import ch.fhnw.bacnetit.transportbinding.ws.WSConnectionHandler;
import ch.fhnw.bacnetit.transportbinding.ws.outgoing.WSConnection;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class WSSConnection implements StatefulConnectionClient {
    private static final InternalLogger LOG = InternalLoggerFactory
            .getInstance(WSConnection.class);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    protected WSConnectionHandler webSocketClientHandler;
    protected Channel channel;
    private final InetSocketAddress remoteAddress;
    private final KeystoreConfig keystoreConfig;
    private final TruststoreConfig truststoreConfig;

    public WSSConnection(final InetSocketAddress remoteAddress,
            final String subprotocol, final KeystoreConfig keystoreConfig,
            final TruststoreConfig truststoreConfig) {
        this.remoteAddress = remoteAddress;
        this.keystoreConfig = keystoreConfig;
        this.truststoreConfig = truststoreConfig;
        try {
            final HttpHeaders httpHeaders = new DefaultHttpHeaders();
            final WebSocketClientHandshaker webSocketClientHandshaker = WebSocketClientHandshakerFactory
                    .newHandshaker(
                            new URI("wss://" + remoteAddress.getHostString()
                                    + ":" + remoteAddress.getPort()
                                    + "/websocket"),
                            WebSocketVersion.V13, subprotocol, false,
                            httpHeaders);
            webSocketClientHandler = new WSConnectionHandler(
                    webSocketClientHandshaker);
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        workerGroup.shutdownGracefully();
    }

    @Override
    public SocketAddress getAddress() {
        return remoteAddress;
    }

    @Override
    public URI getURI() {
        // TODO check correctness and need of method -> getURI is actually never
        // used
        try {
            return new URI("wss://" + remoteAddress.getHostName() + ":"
                    + remoteAddress.getPort());
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setChannel(final Channel channel) {
        this.channel = channel;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public ChannelHandler[] getChannelHandlers() {
        final List<ChannelHandler> handlers = new ArrayList<ChannelHandler>();

        /*
         * Set up initial pipeline. This will be changed after the websocket
         * handshake was successful. {@link WebSocketClientHandler}
         */// private final ConnectionConfig config;
          // Add SSL handler
        try (final InputStream readStreamk = new FileInputStream(
                keystoreConfig.path)) {
            SslContext sslCtx = null;
            SSLEngine engine = null;

            // Trust manager has its own implementation
            final KeyStore ks = KeyStore.getInstance("JKS");

            ks.load(readStreamk, keystoreConfig.pass.toCharArray());

            final KeyManagerFactory kmf = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keystoreConfig.pass.toCharArray());

            final KeyStore ts = KeyStore.getInstance("JKS");
            final InputStream readStream = new FileInputStream(
                    truststoreConfig.path);
            ts.load(readStream, truststoreConfig.pass.toCharArray());
            readStream.close();

            final TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());

            tmf.init(ts);

            sslCtx = SslContextBuilder.forClient().trustManager(tmf)
                    .keyManager(kmf).build();

            // ByteBufAllocator ba = ByteBufAllocator.DEFAULT
            // engine = sslCtx.newEngine(channel.alloc());
            engine = sslCtx.newEngine(ByteBufAllocator.DEFAULT);

            final SslHandler sslHandler = new SslHandler(engine);
            sslHandler.handshakeFuture().addListener(
                    new GenericFutureListener<Future<? super Channel>>() {

                        @Override
                        public void operationComplete(
                                final Future<? super Channel> future)
                                throws Exception {
                            if (!future.isSuccess()) {
                                System.err.println("the fail" + future.cause());

                                new ExceptionManager()
                                        .manageException(new Exception(
                                                "SSL Handshake not completed successful"),

                                                null, null, null);
                                channel.close();
                            }
                        }
                    });
            handlers.add(sslHandler);
        } catch (final Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }

        // pipeline printer
        // handlers.add(new PipelineLogger()); // TODO remove this logger or
        // move its output to logger
        handlers.add(new HttpClientCodec());
        handlers.add(new HttpObjectAggregator(8192));
        // handlers.add(new ReadTimeoutHandler());
        handlers.add(webSocketClientHandler);

        handlers.add(new LoggingHandler(LogLevel.DEBUG));

        return handlers.toArray(new ChannelHandler[handlers.size()]);
    }

    @Override
    public void initialize() {
        try {
            final ChannelFuture cf = webSocketClientHandler.handshakeFuture();
            cf.addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(final ChannelFuture future)
                        throws Exception {
                    if (!future.isSuccess()) {
                        LOG.error(
                                "Error during initialization of the websocket connection");
                        
                    } else {
                        LOG.debug("Websocket connection initialized");
                       
                    }
                }
            });
            cf.await(1000, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            throw new StackInitializationException(e.getMessage(), null);
        }
    }

}
