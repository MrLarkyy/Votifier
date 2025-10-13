package gg.aquatic.votifier.protocol

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import java.nio.charset.StandardCharsets


object ProtocolDifferentiator : ByteToMessageDecoder() {
    override fun decode(
        ctx: ChannelHandlerContext,
        p1: ByteBuf?,
        p2: List<Any?>?
    ) {
        ctx.pipeline().addAfter(
            "protocolDifferentiator",
            "protocol2LengthDecoder",
            LengthFieldBasedFrameDecoder(1024, 2, 2, 0, 4)
        )
        ctx.pipeline()
            .addAfter("protocol2LengthDecoder", "protocol2StringDecoder", StringDecoder(StandardCharsets.UTF_8))
        ctx.pipeline().addAfter("protocol2StringDecoder", "protocol2VoteDecoder", Decoder())
        ctx.pipeline().addAfter("protocol2VoteDecoder", "protocol2StringEncoder", StringEncoder(StandardCharsets.UTF_8))
        ctx.pipeline().remove(this)
    }
}