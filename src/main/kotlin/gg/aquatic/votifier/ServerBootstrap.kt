package gg.aquatic.votifier

import com.sun.org.slf4j.internal.LoggerFactory
import gg.aquatic.votifier.protocol.GreetingHandler
import gg.aquatic.votifier.protocol.InboundHandler
import gg.aquatic.votifier.protocol.ProtocolDifferentiator
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.FastThreadLocalThread
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

class ServerBootstrap(
    val host: String,
    val port: Int,
    private val listener: VotifierListener
) {
    companion object {

        val LOGGER = LoggerFactory.getLogger(ServerBootstrap::class.java)
        private fun createThreadFactory(name: String): ThreadFactory {
            return ThreadFactory { runnable: Runnable? ->
                val thread = FastThreadLocalThread(runnable, name)
                thread.setDaemon(true)
                thread
            }
        }
    }

    var serverChannel: Channel? = null

    private val useEpoll = Epoll.isAvailable()
    val bossLoopGroup: IoEventLoopGroup

    val eventLoopGroup: IoEventLoopGroup

    init {
        val factory = if (useEpoll) {
            EpollIoHandler.newFactory()
        } else {
            NioIoHandler.newFactory()
        }

        bossLoopGroup = MultiThreadIoEventLoopGroup(
            1,
            createThreadFactory("Votifier epoll boss"),
            factory
        )
        eventLoopGroup = MultiThreadIoEventLoopGroup(
            3, createThreadFactory("Votifier epoll worker"), factory
        )
    }

    fun start(error: (Throwable?) -> Unit) {
        Objects.requireNonNull<Any?>(error, "error")

        val voteInboundHandler = InboundHandler(listener)

        ServerBootstrap()
            .channel(if (useEpoll) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java)
            .group(bossLoopGroup, eventLoopGroup)
            .childHandler(object : ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
                override fun initChannel(channel: io.netty.channel.socket.SocketChannel) {
                    channel.attr(Session.KEY).set(Session())
                    channel.pipeline().addLast("greetingHandler", GreetingHandler)
                    channel.pipeline()
                        .addLast("protocolDifferentiator", ProtocolDifferentiator)
                    channel.pipeline().addLast("voteHandler", voteInboundHandler)
                }
            })
            .bind(host, port)
            .addListener(ChannelFutureListener { future: ChannelFuture? ->
                if (future!!.isSuccess) {
                    serverChannel = future.channel()
                    LOGGER.trace("Votifier enabled on socket " + serverChannel?.localAddress() + ".")
                    error(null)
                } else {
                    var socketAddress = future.channel().localAddress()
                    if (socketAddress == null) {
                        socketAddress = InetSocketAddress(host, port)
                    }
                    LOGGER
                        .error("Votifier was not able to bind to $socketAddress", future.cause())
                    error(future.cause())
                }
            })
    }

    fun shutdown() {
        serverChannel?.apply {
            try {
                close().syncUninterruptibly()
            } catch (e: Exception) {
                LOGGER.error("Unable to shutdown server channel", e)
            }
        }
        eventLoopGroup.shutdownGracefully()
        bossLoopGroup.shutdownGracefully()

        try {
            bossLoopGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
            eventLoopGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}