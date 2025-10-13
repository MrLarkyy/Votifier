package gg.aquatic.votifier.protocol

import gg.aquatic.votifier.Session
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.nio.charset.StandardCharsets


@ChannelHandler.Sharable
object GreetingHandler: ChannelInboundHandlerAdapter() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        val session = ctx.channel().attr(Session.KEY).get()
        val version = "VOTIFIER 2 " + session.challenge + "\n"
        val versionBuf = Unpooled.copiedBuffer(version, StandardCharsets.UTF_8)
        ctx.writeAndFlush(versionBuf)
    }
}