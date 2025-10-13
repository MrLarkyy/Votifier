package gg.aquatic.votifier

import java.math.BigInteger
import java.security.SecureRandom

private val RANDOM = SecureRandom()

fun createToken(): String {
    return BigInteger(130, RANDOM).toString(32)
}