package mqtt.packets.mqtt

import mqtt.packets.ConnectFlags
import mqtt.packets.MQTTPacket

abstract class MQTTConnect(
    val protocolName: String,
    val protocolVersion: Int,
    val connectFlags: ConnectFlags,
    val keepAlive: Int,
    val clientID: String = "",
    val willTopic: String? = null,
    val willPayload: UByteArray? = null,
    val userName: String? = null,
    val password: UByteArray? = null
) : MQTTPacket
