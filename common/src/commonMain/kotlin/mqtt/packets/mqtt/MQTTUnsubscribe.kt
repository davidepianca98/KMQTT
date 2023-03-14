package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

abstract class MQTTUnsubscribe(
    val packetIdentifier: UInt,
    val topicFilters: List<String>
) : MQTTPacket
