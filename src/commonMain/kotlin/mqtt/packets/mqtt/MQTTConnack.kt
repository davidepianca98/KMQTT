package mqtt.packets.mqtt

import mqtt.packets.ConnectAcknowledgeFlags
import mqtt.packets.MQTTPacket

abstract class MQTTConnack(val connectAcknowledgeFlags: ConnectAcknowledgeFlags) : MQTTPacket
