package gg.aquatic.votifier.protocol

import com.google.gson.JsonParser
import gg.aquatic.votifier.Handler
import gg.aquatic.votifier.Session
import gg.aquatic.votifier.Vote
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.CorruptedFrameException
import io.netty.handler.codec.MessageToMessageDecoder
import java.security.InvalidKeyException
import java.security.Key
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class Decoder : MessageToMessageDecoder<String>() {

    companion object {
        private val RANDOM = SecureRandom()
    }

    override fun decode(ctx: ChannelHandlerContext, s: String, list: MutableList<Any>) {
        val voteMessage = JsonParser.parseString(s).asJsonObject
        val session = ctx.channel().attr(Session.KEY).get()

        val votePayload = voteMessage.get("payload").asJsonObject
        if (votePayload.get("challenge").asString != session.challenge ) {
            throw CorruptedFrameException("Challenge does not match session challenge.")
        }

        val serviceName = votePayload.get("serviceName").asString
        val key = Handler.tokens[serviceName] ?: Handler.tokens["default"]
        if (key == null) {
            throw CorruptedFrameException("Service name is not registered.")
        }

        val sigHash = votePayload.get("signature").asString
        val sigBytes = Base64.getDecoder().decode(sigHash)

        if (!hmacEqual(sigBytes, votePayload.asString.toByteArray(Charsets.UTF_8),key)) {
            throw CorruptedFrameException("Signature does not match.")
        }

        if (votePayload.has("uuid")) {
            UUID.fromString(votePayload.get("uuid").asString)
        }

        val userName = votePayload.get("username").asString
        if (userName.length > 16) {
            throw CorruptedFrameException("Username is too long.")
        }

        val vote = Vote(userName, serviceName, votePayload)
        list += vote

        ctx.pipeline().remove(this)
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    private fun hmacEqual(sig: ByteArray, message: ByteArray, key: Key): Boolean {
        // See https://www.nccgroup.trust/us/about-us/newsroom-and-events/blog/2011/february/double-hmac-verification/
        // This randomizes the byte order to make timing attacks more difficult.
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        val calculatedSig = mac.doFinal(message)

        // Generate a random key for use in comparison
        val randomKey = ByteArray(32)
        RANDOM.nextBytes(randomKey)

        // Then generate two HMACs for the different signatures found
        val mac2 = Mac.getInstance("HmacSHA256")
        mac2.init(SecretKeySpec(randomKey, "HmacSHA256"))
        val clientSig = mac2.doFinal(sig)
        mac2.reset()
        val realSig = mac2.doFinal(calculatedSig)

        return MessageDigest.isEqual(clientSig, realSig)
    }

}