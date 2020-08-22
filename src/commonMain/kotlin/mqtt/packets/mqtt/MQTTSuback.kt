package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

abstract class MQTTSuback(val packetIdentifier: UInt) : MQTTPacket
