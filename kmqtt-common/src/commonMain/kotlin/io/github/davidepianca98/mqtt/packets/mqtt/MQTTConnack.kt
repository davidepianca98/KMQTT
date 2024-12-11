package io.github.davidepianca98.mqtt.packets.mqtt

import io.github.davidepianca98.mqtt.packets.ConnectAcknowledgeFlags
import io.github.davidepianca98.mqtt.packets.MQTTPacket

public abstract class MQTTConnack(public val connectAcknowledgeFlags: ConnectAcknowledgeFlags) : MQTTPacket
