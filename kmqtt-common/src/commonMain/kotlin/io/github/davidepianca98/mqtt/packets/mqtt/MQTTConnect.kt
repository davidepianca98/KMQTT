package io.github.davidepianca98.mqtt.packets.mqtt

import io.github.davidepianca98.mqtt.packets.ConnectFlags
import io.github.davidepianca98.mqtt.packets.MQTTPacket

public abstract class MQTTConnect(
    public val protocolName: String,
    public val protocolVersion: Int,
    public val connectFlags: ConnectFlags,
    public val keepAlive: Int,
    public val clientID: String = "",
    public val willTopic: String? = null,
    public val willPayload: UByteArray? = null,
    public val userName: String? = null,
    public val password: UByteArray? = null
) : MQTTPacket
