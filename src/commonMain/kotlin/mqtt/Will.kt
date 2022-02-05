package mqtt

import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTConnect
import mqtt.packets.mqttv5.MQTT5Connect
import mqtt.packets.mqttv5.ReasonCode
import validatePayloadFormat

class Will(packet: MQTTConnect) {
    val retain: Boolean
    val qos: Qos
    val topic: String
    val payload: UByteArray
    val willDelayInterval: UInt
    val payloadFormatIndicator: UInt
    val messageExpiryInterval: UInt?
    val contentType: String?
    val responseTopic: String?
    val correlationData: UByteArray?
    val userProperty: List<Pair<String, String>>

    init {
        val properties = if (packet is MQTT5Connect) packet.willProperties!! else null
        val formatIndicator = properties?.payloadFormatIndicator ?: 0u
        if (packet.willPayload?.validatePayloadFormat(formatIndicator) == false)
            throw MQTTException(ReasonCode.PAYLOAD_FORMAT_INVALID)
        this.retain = packet.connectFlags.willRetain
        this.qos = packet.connectFlags.willQos
        this.topic = packet.willTopic!!
        this.payload = packet.willPayload!!
        this.willDelayInterval = properties?.willDelayInterval ?: 0u
        this.payloadFormatIndicator = formatIndicator
        this.messageExpiryInterval = properties?.messageExpiryInterval
        this.contentType = properties?.contentType
        this.responseTopic = properties?.responseTopic
        this.correlationData = properties?.correlationData
        this.userProperty = properties?.userProperty ?: listOf()
    }
}
