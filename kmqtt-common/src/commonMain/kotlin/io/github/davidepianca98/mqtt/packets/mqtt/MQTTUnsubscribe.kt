package io.github.davidepianca98.mqtt.packets.mqtt

import io.github.davidepianca98.mqtt.packets.MQTTPacket

public abstract class MQTTUnsubscribe(
    public val packetIdentifier: UInt,
    public val topicFilters: List<String>
) : MQTTPacket
