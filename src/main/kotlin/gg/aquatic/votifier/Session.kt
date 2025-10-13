package gg.aquatic.votifier

import io.netty.util.AttributeKey

class Session {

    companion object {
        val KEY = AttributeKey.valueOf<Session>("votifier_session")
    }

    val challenge = createToken()
    var hasCompletedVote = false

    fun completeVote() {
        require(!hasCompletedVote) { "Cannot complete a vote more than once" }
        hasCompletedVote = true
    }
}