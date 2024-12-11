package io.github.davidepianca98.mqtt.packets.mqtt

import io.github.davidepianca98.mqtt.Subscription
import io.github.davidepianca98.mqtt.packets.MQTTPacket

public abstract class MQTTSubscribe(
    public val packetIdentifier: UInt,
    public val subscriptions: List<Subscription>
) : MQTTPacket
