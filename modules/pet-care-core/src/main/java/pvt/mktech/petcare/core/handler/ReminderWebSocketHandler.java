package pvt.mktech.petcare.core.handler;

import cn.hutool.json.JSONUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.core.dto.message.ReminderExecutionMessageDto;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static pvt.mktech.petcare.common.constant.CommonConstant.*;

/**
 * 提醒 WebSocket 处理器
 * 基于 Netty 实现，管理 WebSocket 连接和消息发送
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ReminderWebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private final Map<Long, Channel> userChannels = new ConcurrentHashMap<>();
    private final Map<Channel, Long> channelUsers = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!req.decoderResult().isSuccess()) {
            ctx.close();
            return;
        }

        Long userId = authenticateRequest(req);
        if (userId == null) {
            log.warn("WebSocket 连接认证失败");
            ctx.close();
            return;
        }

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, false);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
            ctx.channel().attr(io.netty.util.AttributeKey.valueOf("handshaker")).set(handshaker);
            onConnectionEstablished(ctx.channel(), userId);
        }
    }

    private Long authenticateRequest(FullHttpRequest req) {
        // 1. 优先从 Header 获取用户ID（Gateway 已验证并添加）
        String userIdHeader = req.headers().get(HEADER_USER_ID);
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                log.info("从 Gateway Header 获取用户ID: {}", userId);
                return userId;
            } catch (NumberFormatException e) {
                log.warn("Header 中的用户ID格式错误: {}", userIdHeader);
            }
        }
        return null;
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            WebSocketServerHandshaker handshaker = ctx.channel()
                    .attr(io.netty.util.AttributeKey.<WebSocketServerHandshaker>valueOf("handshaker")).get();
            if (handshaker != null) {
                handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            }
            onConnectionClosed(ctx.channel());
            return;
        }

        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            log.debug("收到消息: {}", text);

            if ("ping".equals(text)) {
                ctx.channel().writeAndFlush(new TextWebSocketFrame("pong"));
            }
        }
    }

    private void onConnectionEstablished(Channel channel, Long userId) {
        Channel oldChannel = userChannels.get(userId);
        if (oldChannel != null && oldChannel.isActive()) {
            oldChannel.close();
        }

        userChannels.put(userId, channel);
        channelUsers.put(channel, userId);
        log.info("用户 {} WebSocket 连接已建立，当前在线用户数: {}", userId, userChannels.size());
    }

    private void onConnectionClosed(Channel channel) {
        Long userId = channelUsers.remove(channel);
        if (userId != null) {
            userChannels.remove(userId);
            log.info("用户 {} WebSocket 连接已关闭", userId);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        onConnectionClosed(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("WebSocket 异常", cause);
        ctx.close();
    }


    private String getWebSocketLocation(FullHttpRequest req) {
        String location = req.headers().get(HttpHeaderNames.HOST) + "/ws/reminders";
        return "ws://" + location;
    }

    /**
     * 向指定用户发送提醒消息
     */
    public void sendReminderToUser(Long userId, ReminderExecutionMessageDto message) throws IOException {
        Channel channel = userChannels.get(userId);

        if (channel != null && channel.isActive()) {
            String jsonMessage = JSONUtil.toJsonStr(message);
            channel.writeAndFlush(new TextWebSocketFrame(jsonMessage));
            log.info("成功向用户 {} 发送提醒消息", userId);
        } else {
            log.warn("用户 {} 的 WebSocket 连接不存在或已关闭，当前在线用户: {}", userId, userChannels.keySet());
        }
    }

    /**
     * 获取所有在线用户ID
     */
    public java.util.Set<Long> getOnlineUsers() {
        return userChannels.keySet();
    }
}

