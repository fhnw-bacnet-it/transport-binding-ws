package ch.fhnw.bacnetit.binding.ws.incoming;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;

import ch.fhnw.bacnetit.binding.ws.ControlMessageHandler;
import ch.fhnw.bacnetit.binding.ws.WSEncoder;
import ch.fhnw.bacnetit.binding.ws.outgoing.WSConnection;
import ch.fhnw.bacnetit.stack.application.auth.http.HttpBasicAuthHandler;
import ch.fhnw.bacnetit.stack.application.configuration.HttpAuthConfig;
import ch.fhnw.bacnetit.stack.application.transaction.ChannelEvent;
import ch.fhnw.bacnetit.stack.encoding.ControlMessageInitEvent;
import ch.fhnw.bacnetit.stack.encoding.TPDU;
import ch.fhnw.bacnetit.stack.network.directory.DirectoryService;
import ch.fhnw.bacnetit.stack.network.transport.ConnectionClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Handles handshakes and messages
 */
public class WSConnectionServerHandler
        extends SimpleChannelInboundHandler<Object> {
    private static final InternalLogger LOG = InternalLoggerFactory
            .getInstance(WSConnectionServerHandler.class);
    private static final String WEBSOCKET_PATH = "/websocket";
    private static WebSocketServerHandshaker handshaker;
    private String secProtocol = null;
    private HttpAuthConfig httpAuthConfig = null;

    public void setHttpAuthConfig(final HttpAuthConfig config) {
        this.httpAuthConfig = config;
    }

    public void setSecProtocol(final String secProtocol) {
        this.secProtocol = secProtocol;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final Object msg)
            throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
        ctx.flush();
    }

    // Receive HTTP Request
    private void handleHttpRequest(final ChannelHandlerContext ctx,
            final FullHttpRequest req) {
        LOG.debug("Received HTTP request " + req);
        if (req.headers().contains("Upgrade")) {
            establishWebSocketConnection(ctx, req);
        }
    }

    private void establishWebSocketConnection(final ChannelHandlerContext ctx,
            final FullHttpRequest req) {

        // When stack.httpauth is true, the HttpBasicAuthHandler must handle the
        // Basic Auth Request
        if (httpAuthConfig != null) {
            final Set<String> httpBasicAuthSessions = HttpBasicAuthHandler
                    .getHttpBasicAuthSessions();
            LOG.info("Amount entries in session space "
                    + httpBasicAuthSessions.size());
            if (req.headers().get("Cookie") == null) {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
                LOG.error(
                        "stack.httpauth is on. Faild to establish connection without cookie");
                return;
            }

            // Trim the cookie
            final String receivedCookie = Arrays
                    .asList(req.headers().get("Cookie").trim().split(";"))
                    .stream().filter(s -> s.toLowerCase().contains("sid="))
                    .findFirst().get().trim();
            LOG.info("trimmed cookie: " + receivedCookie);

            if (!httpBasicAuthSessions.contains(receivedCookie)) {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
                LOG.error(
                        "stack.httpauth is on. Faild to establish connection without valid login");
                return;
            } else {
                LOG.info(
                        "stack.httpauth is on. Establish connection with valid login");
            }

        }

        // WebSocketHandshake
        final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), secProtocol, true);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory
                    .sendUnsupportedVersionResponse(ctx.channel());
        } else {
            final ChannelFuture cf = handshaker.handshake(ctx.channel(), req);
            cf.addListener(new ChannelFutureListener() {
                // Serverside pipeline changes after WebSocket Upgrade
                @Override
                public void operationComplete(final ChannelFuture future)
                        throws Exception {

                    LOG.debug(
                            "WebSocket Server Handshake completed successfully");
                    // Adding the ControlMessageHandler
                    ctx.pipeline().addBefore(
                            WSConnectionServerHandler.class.getSimpleName(),
                            ControlMessageHandler.class.getSimpleName(),
                            new ControlMessageHandler());
                    ctx.pipeline().addAfter(
                            WSConnectionServerHandler.class.getSimpleName(),
                            WSEncoder.class.getSimpleName(), new WSEncoder());
                    ctx.pipeline().fireUserEventTriggered(
                            new ControlMessageInitEvent());

                }
            });
        }
    }

    private void handleWebSocketFrame(final ChannelHandlerContext ctx,
            final WebSocketFrame frame) throws Exception {
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(),
                    (CloseWebSocketFrame) frame.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel()
                    .write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (!(frame instanceof BinaryWebSocketFrame)) {
            throw new UnsupportedOperationException(
                    String.format("%s frame types not supported",
                            frame.getClass().getName()));
        }
        final ByteBuf buffer = frame.content();
        buffer.skipBytes(2);
        final byte[] bytes = new byte[buffer.readableBytes()];

        // move removing upside
        buffer.readBytes(bytes);

        // Translate here to BACnetMessage
        final TPDU bmsg = new TPDU(bytes);
        System.out.println("does a invoke id exists no4?:");
        System.out.println(bmsg.getInvokeId());
        LOG.debug("Successfully decoded new BACnetMessage: " + bmsg);

        // Fire event up to endpoint handler to signal a client connection
        final InetSocketAddress address = (InetSocketAddress) ctx.channel()
                .remoteAddress();

        final ConnectionClient connection = new WSConnection(address, null);
        connection.setChannel(ctx.channel());
        ctx.pipeline().fireUserEventTriggered(connection);
        LOG.debug("Register a incoming WebSocketFrame in the local dns");
        try {
            LOG.debug(String.format("SourceEID %s, URI: URI: %S",
                    bmsg.getSourceEID(),
                    new URI("ws:/" + ctx.channel().remoteAddress())));
        } catch (final URISyntaxException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            DirectoryService.getInstance().register(bmsg.getSourceEID(),
                    new URI("ws:/" + ctx.channel().remoteAddress()), false,
                    true);
        } catch (final URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Put message back into the pipeline
        ctx.fireChannelRead(bmsg);
    }

    private static void sendHttpResponse(final ChannelHandlerContext ctx,
            final FullHttpRequest req, final FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        // if (res.getStatus().code() != 200) {
        final ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(),
                CharsetUtil.UTF_8);
        res.content().writeBytes(buf);
        buf.release();
        HttpUtil.setContentLength(res, res.content().readableBytes());
        // }

        // Send the response and close the connection if necessary.
        final ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
            final Throwable cause) {
        Throwable _cause;
        if (cause.getCause() instanceof javax.net.ssl.SSLHandshakeException
                || cause instanceof javax.net.ssl.SSLHandshakeException) {
            _cause = new Exception(cause.getMessage());
        } else {
            _cause = cause;
        }
        ctx.fireExceptionCaught(_cause);
        ctx.close();
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx)
            throws Exception {
        LOG.debug("UnregisterEvent reveived from "
                + ctx.channel().remoteAddress());
        final ChannelEvent event = ChannelEvent.REMOVE_CONNECTION_EVENT;
        event.setMsg(ctx.channel().remoteAddress());
        ctx.fireUserEventTriggered(event);
    }

    private String getWebSocketLocation(final FullHttpRequest req) {
        final String location = req.headers().get(HttpHeaderNames.HOST)
                + WEBSOCKET_PATH;
        return "ws://" + location;
    }
}
