package gg.aquatic.votifier.protocol

import com.google.gson.JsonObject
import gg.aquatic.votifier.Session
import gg.aquatic.votifier.Vote
import gg.aquatic.votifier.VotifierListener
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.util.concurrent.atomic.AtomicLong

@ChannelHandler.Sharable
class InboundHandler(
    private val listener: VotifierListener
): SimpleChannelInboundHandler<Vote>() {

    private val lastError: AtomicLong = AtomicLong()
    private val errorsSent: AtomicLong = AtomicLong()

    override fun channelRead0(ctx: ChannelHandlerContext, vote: Vote) {
        val session = ctx.channel().attr(Session.KEY).get()

        listener.onVoteReceived(vote, ctx.channel().remoteAddress().toString())
        session.completeVote()

        val obj = JsonObject()
        obj.addProperty("status", "ok")
        ctx.writeAndFlush(obj.toString() + "\r\n").addListener(ChannelFutureListener.CLOSE)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        val session = ctx.channel().attr(Session.KEY).get()

        val remoteAddr: String = ctx.channel().remoteAddress().toString()
        val hasCompletedVote: Boolean = session.hasCompletedVote

        val obj = JsonObject()
        obj.addProperty("status", "error")
        obj.addProperty("cause", cause.javaClass.getSimpleName())
        obj.addProperty("error", cause.message)
        ctx.writeAndFlush(obj.toString() + "\r\n").addListener(ChannelFutureListener.CLOSE)

        if (!willThrottleErrorLogging()) {
            listener.onError(cause, hasCompletedVote, remoteAddr)
        }
    }

    private fun willThrottleErrorLogging(): Boolean {
        val lastErrorAt: Long = this.lastError.get()
        val now = System.currentTimeMillis()

        if (lastErrorAt + 2000 >= now) {
            return this.errorsSent.incrementAndGet() >= 5
        } else {
            this.lastError.set(now)
            this.errorsSent.set(0)
            return false
        }
    }
}