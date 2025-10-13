package gg.aquatic.votifier

import com.google.gson.JsonObject
import java.util.Base64

class Vote(
    val serviceName: String,
    val userName: String,
    val address: String,
    val timestamp: Long,
    val additionalData: ByteArray?
) {

    constructor(serviceName: String, userName: String, address: String, timestamp: Long) : this(
        serviceName,
        userName,
        address,
        timestamp,
        null
    )

    constructor(vote: Vote) : this(
        vote.serviceName,
        vote.userName,
        vote.address,
        vote.timestamp,
        vote.additionalData?.clone()
    )

    constructor(userName: String, serviceName: String, jsonObject: JsonObject) : this(
        serviceName,
        userName,
        jsonObject.get("address").asString,
        jsonObject.get("timeStamp").asString.toLong(),
        if (jsonObject.has("additionalData")) Base64.getDecoder()
            .decode(jsonObject.get("additionalData").asString) else null
    )

}