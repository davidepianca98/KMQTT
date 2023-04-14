package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

public abstract class MQTTUnsubscribe(
    public val packetIdentifier: UInt,
    public val topicFilters: List<String>
) : MQTTPacket
