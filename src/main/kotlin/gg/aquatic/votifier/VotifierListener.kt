package gg.aquatic.votifier

interface VotifierListener {
    fun onVoteReceived(vote: Vote, remoteAddress: String)
    fun onError(throwable: Throwable, voteAlreadyCompleted: Boolean, remoteAddress: String)
}
