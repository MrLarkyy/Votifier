# Votifier

A lightweight, high-performance Kotlin implementation of the Votifier protocol (v2) built on Netty. This library allows you to receive and verify votes from Minecraft server listing websites.

## Features
- **Secure:** Uses HMAC-SHA256 signatures for vote verification.
- **Replay Protection:** Unique session challenges (nonces) for every connection.
- **Developer Friendly:** Simple listener-based API.
- **High Performance:** Asynchronous networking powered by Netty (with Epoll support on Linux).

---

## Getting Started

### 1. Implement the Listener
To handle incoming votes and potential errors, implement the `VotifierListener` interface.

```kotlin
class MyVoteHandler : VotifierListener {
    override fun onVoteReceived(vote: Vote, remoteAddress: String) {
        println("Received vote for ${vote.username} from ${vote.serviceName}")
        // Grant your in-game rewards here!
    }

    override fun onError(throwable: Throwable, voteAlreadyCompleted: Boolean, remoteAddress: String) {
        System.err.println("Error processing vote from $remoteAddress: ${throwable.message}")
    }
}
```


### 2. Configure Tokens
Tokens are shared secrets between your server and the voting website. You can register specific tokens for different services.

```kotlin
// Add a specific token for a voting site
Handler.addToken("PlanetMinecraft.com", "your_secret_token_here")

// Or use the default token for any unregistered service
Handler.tokens["default"] = Handler.createKeyFrom("my_global_secret")
```


### 3. Start the Server
Pass your listener and configuration to the `ServerBootstrap`.

```kotlin
val server = ServerBootstrap(
    host = "0.0.0.0",
    port = 8192,
    listener = MyVoteHandler()
)

server.start { error ->
    if (error != null) {
        println("Failed to start Votifier: ${error.message}")
    } else {
        println("Votifier is now listening for votes!")
    }
}
```

---

## Protocol Overview
When a client (a voting website) connects:
1. **Challenge:** The server sends a unique `challenge` string to the client.
2. **Signature:** The client must send back a JSON payload containing the vote details, the `challenge`, and an HMAC-SHA256 `signature`.
3. **Verification:** The server looks up the token for the `serviceName`, verifies the signature, and ensures the challenge matches the session.

