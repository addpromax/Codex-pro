package net.momirealms.craftengine.core.pack.host.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SelfHostHttpServer {
    private static SelfHostHttpServer instance;
    private final Cache<String, Boolean> oneTimePackUrls = Caffeine.newBuilder()
            .maximumSize(256)
            .scheduler(Scheduler.systemScheduler())
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();
    private final Cache<String, IpAccessRecord> ipAccessCache = Caffeine.newBuilder()
            .maximumSize(256)
            .scheduler(Scheduler.systemScheduler())
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong blockedRequests = new AtomicLong();

    private int rateLimit = 1;
    private long rateLimitInterval = 1000;
    private String ip = "localhost";
    private int port = -1;
    private String protocol = "http";
    private String url;
    private boolean denyNonMinecraft = true;
    private boolean useToken;

    private byte[] resourcePackBytes;
    private String packHash;
    private UUID packUUID;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public static SelfHostHttpServer instance() {
        if (instance == null) {
            instance = new SelfHostHttpServer();
        }
        return instance;
    }

    public void updateProperties(String ip,
                                 int port,
                                 String url,
                                 boolean denyNonMinecraft,
                                 String protocol,
                                 int maxRequests,
                                 int resetInterval,
                                 boolean token) {
        this.ip = ip;
        this.url = url;
        this.denyNonMinecraft = denyNonMinecraft;
        this.protocol = protocol;
        this.rateLimit = maxRequests;
        this.rateLimitInterval = resetInterval;
        this.useToken = token;

        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }

        if (this.port == port && serverChannel != null) return;
        disable();

        this.port = port;
        initializeServer();
    }

    public String url() {
        if (this.url != null && !this.url.isEmpty()) {
            return this.url;
        }
        return this.protocol + "://" + this.ip + ":" + this.port + "/";
    }

    private void initializeServer() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(1048576));
                        pipeline.addLast(new RequestHandler());
                    }
                });
        try {
            serverChannel = b.bind(port).sync().channel();
            CraftEngine.instance().logger().info("Netty HTTP server started on port: " + port);
        } catch (InterruptedException e) {
            CraftEngine.instance().logger().warn("Failed to start Netty server", e);
            Thread.currentThread().interrupt();
        }
    }

    @ChannelHandler.Sharable
    private class RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            totalRequests.incrementAndGet();

            try {
                String clientIp = ((InetSocketAddress) ctx.channel().remoteAddress())
                        .getAddress().getHostAddress();

                if (checkRateLimit(clientIp)) {
                    sendError(ctx, HttpResponseStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
                    blockedRequests.incrementAndGet();
                    return;
                }

                QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
                String path = queryDecoder.path();

                if ("/download".equals(path)) {
                    handleDownload(ctx, request, queryDecoder);
                } else if ("/metrics".equals(path)) {
                    handleMetrics(ctx);
                } else {
                    sendError(ctx, HttpResponseStatus.NOT_FOUND, "Not Found");
                }
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Request handling failed", e);
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Error");
            }
        }

        private void handleDownload(ChannelHandlerContext ctx, FullHttpRequest request, QueryStringDecoder queryDecoder) {
            if (useToken) {
                String token = queryDecoder.parameters().getOrDefault("token", java.util.Collections.emptyList()).stream().findFirst().orElse(null);
                if (!validateToken(token)) {
                    sendError(ctx, HttpResponseStatus.FORBIDDEN, "Invalid token");
                    blockedRequests.incrementAndGet();
                    return;
                }
            }

            if (denyNonMinecraft) {
                String userAgent = request.headers().get(HttpHeaderNames.USER_AGENT);
                if (userAgent == null || !userAgent.startsWith("Minecraft Java/")) {
                    sendError(ctx, HttpResponseStatus.FORBIDDEN, "Invalid client");
                    blockedRequests.incrementAndGet();
                    return;
                }
            }

            if (resourcePackBytes == null) {
                sendError(ctx, HttpResponseStatus.NOT_FOUND, "Resource pack missing");
                blockedRequests.incrementAndGet();
                return;
            }

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(resourcePackBytes)
            );
            response.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, "application/zip")
                    .set(HttpHeaderNames.CONTENT_LENGTH, resourcePackBytes.length);

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private void handleMetrics(ChannelHandlerContext ctx) {
            String metrics = "# TYPE total_requests counter\n"
                    + "total_requests " + totalRequests.get() + "\n"
                    + "# TYPE blocked_requests counter\n"
                    + "blocked_requests " + blockedRequests.get();

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(metrics, CharsetUtil.UTF_8)
            );
            response.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, "text/plain")
                    .set(HttpHeaderNames.CONTENT_LENGTH, metrics.length());

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private boolean checkRateLimit(String clientIp) {
            IpAccessRecord record = ipAccessCache.getIfPresent(clientIp);
            long now = System.currentTimeMillis();

            if (record == null) {
                record = new IpAccessRecord(now, 1);
                ipAccessCache.put(clientIp, record);
                return false;
            }

            if (now - record.lastAccessTime > rateLimitInterval) {
                record.lastAccessTime = now;
                record.accessCount = 1;
                return false;
            }

            return ++record.accessCount > rateLimit;
        }

        private boolean validateToken(String token) {
            if (token == null || token.length() != 36) return false;
            Boolean valid = oneTimePackUrls.getIfPresent(token);
            if (valid != null) {
                oneTimePackUrls.invalidate(token);
                return true;
            }
            return false;
        }

        private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    status,
                    Unpooled.copiedBuffer(message, CharsetUtil.UTF_8)
            );
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }
    }

    @Nullable
    public ResourcePackDownloadData generateOneTimeUrl() {
        if (this.resourcePackBytes == null) return null;

        if (!this.useToken) {
            return new ResourcePackDownloadData(url() + "download", this.packUUID, this.packHash);
        }

        String token = UUID.randomUUID().toString();
        oneTimePackUrls.put(token, true);
        return new ResourcePackDownloadData(
                url() + "download?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8),
                packUUID,
                packHash
        );
    }

    public void disable() {
        if (serverChannel != null) {
            serverChannel.close().awaitUninterruptibly();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serverChannel = null;
        }
    }

    public void readResourcePack(Path path) {
        try {
            if (Files.exists(path)) {
                this.resourcePackBytes = Files.readAllBytes(path);
                calculateHash();
            } else {
                this.resourcePackBytes = null;
            }
        } catch (IOException e) {
            CraftEngine.instance().logger().severe("Failed to load resource pack", e);
        }
    }

    private void calculateHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(this.resourcePackBytes);
            byte[] hashBytes = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            this.packHash = hexString.toString();
            this.packUUID = UUID.nameUUIDFromBytes(this.packHash.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            CraftEngine.instance().logger().severe("SHA-1 algorithm not available", e);
        }
    }

    private static class IpAccessRecord {
        long lastAccessTime;
        int accessCount;

        IpAccessRecord(long lastAccessTime, int accessCount) {
            this.lastAccessTime = lastAccessTime;
            this.accessCount = accessCount;
        }
    }
}