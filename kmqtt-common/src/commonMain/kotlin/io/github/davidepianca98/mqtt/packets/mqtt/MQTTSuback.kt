package io.github.davidepianca98.mqtt.packets.mqtt

import io.github.davidepianca98.mqtt.packets.MQTTPacket

public abstract class MQTTSuback(public val packetIdentifier: UInt) : MQTTPacket
