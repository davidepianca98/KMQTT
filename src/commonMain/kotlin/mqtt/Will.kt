package mqtt

import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTConnect
import mqtt.packets.mqttv5.MQTT5Connect
import mqtt.packets.mqttv5.ReasonCode
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
                val properties = if (packet is MQTT5Connect) packet.willProperties!! else null
                val formatIndicator = properties?.payloadFormatIndicator ?: 0u
                if (packet.willPayload?.validatePayloadFormat(formatIndicator) == false)
                    throw MQTTException(ReasonCode.PAYLOAD_FORMAT_INVALID)
                Will(
                    packet.connectFlags.willRetain,
                    packet.connectFlags.willQos,
                    packet.willTopic!!,
                    packet.willPayload!!,
                    properties?.willDelayInterval ?: 0u,
                    formatIndicator,
                    properties?.messageExpiryInterval,
                    properties?.contentType,
                    properties?.responseTopic,
                    properties?.correlationData,
                    properties?.userProperty ?: listOf()
                )
            } else
                null
        }
    }
}
