package gg.aquatic.votifier

import java.nio.charset.StandardCharsets
import java.security.Key
import javax.crypto.spec.SecretKeySpec



object Handler {

    val tokens = hashMapOf(
        "default" to createKeyFrom(createToken())
    )

    fun addToken(website: String, token: String) {
        tokens[website] = createKeyFrom(token)
    }

    fun onVoteReceived(vote: Vote, remoteAddress: String) {

    }

    fun onError(throwable: Throwable, voteAlreadyCompleted: Boolean, remoteAddress: String) {

    }

    fun createKeyFrom(token: String): Key {
        return SecretKeySpec(token.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
    }
}