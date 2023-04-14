package mqtt.packets.mqtt

import mqtt.packets.ConnectAcknowledgeFlags
import mqtt.packets.MQTTPacket

public abstract class MQTTConnack(public val connectAcknowledgeFlags: ConnectAcknowledgeFlags) : MQTTPacket
