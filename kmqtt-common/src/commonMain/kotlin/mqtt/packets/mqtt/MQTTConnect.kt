package mqtt.packets.mqtt

import mqtt.packets.ConnectFlags
import mqtt.packets.MQTTPacket

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
