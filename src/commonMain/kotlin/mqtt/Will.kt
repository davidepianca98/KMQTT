package mqtt

import mqtt.packets.MQTTConnect
import mqtt.packets.Qos
import mqtt.packets.ReasonCode
import validatePayloadFormat

class Will(
    val retain: Boolean,
    val qos: Qos,
    val topic: String,
    val payload: UByteArray,
    val willDelayInterval: UInt,
    val payloadFormatIndicator: UInt,
    val messageExpiryInterval: UInt?,
    val contentType: String?,
    val responseTopic: String?,
    val correlationData: UByteArray?,
    val userProperty: List<Pair<String, String>>
) {
    companion object {
        fun buildWill(packet: MQTTConnect): Will? {
            return if (packet.connectFlags.willFlag) {
                val formatIndicator = packet.willProperties!!.payloadFormatIndicator ?: 0u
                if (packet.willPayload?.validatePayloadFormat(formatIndicator) == false)
                    throw MQTTException(ReasonCode.PAYLOAD_FORMAT_INVALID)
                Will(
                    packet.connectFlags.willRetain,
                    packet.connectFlags.willQos,
                    packet.willTopic!!,
                    packet.willPayload!!,
                    packet.willProperties.willDelayInterval
                        ?: 0u, // TODO publish will after this interval or when the session ends, first to come, if client reconnects to session don't send
                    formatIndicator,
                    packet.willProperties.messageExpiryInterval,
                    packet.willProperties.contentType,
                    packet.willProperties.responseTopic,
                    packet.willProperties.correlationData,
                    packet.willProperties.userProperty
                )
            } else
                null
        }
    }
}
