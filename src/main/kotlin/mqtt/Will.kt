package mqtt

import mqtt.packets.Qos

class Will(
    val retain: Boolean,
    val qos: Qos,
    val topic: String,
    val payload: ByteArray,
    val willDelayInterval: UInt,
    val payloadFormatIndicator: UInt,
    val messageExpiryInterval: UInt?,
    val contentType: String?,
    val responseTopic: String?,
    val correlationData: ByteArray?,
    val userProperty: List<Pair<String, String>>
)
