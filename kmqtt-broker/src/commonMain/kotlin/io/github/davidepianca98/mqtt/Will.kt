package io.github.davidepianca98.mqtt

import io.github.davidepianca98.mqtt.packets.Qos
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTConnect
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Connect
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import io.github.davidepianca98.validatePayloadFormat

public class Will(packet: MQTTConnect) {
    public val retain: Boolean
    public val qos: Qos
    public val topic: String
    public val payload: UByteArray
    public val willDelayInterval: UInt
    public val payloadFormatIndicator: UInt
    public val messageExpiryInterval: UInt?
    public val contentType: String?
    public val responseTopic: String?
    public val correlationData: UByteArray?
    public val userProperty: List<Pair<String, String>>

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
