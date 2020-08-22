package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

abstract class MQTTUnsuback(val packetIdentifier: UInt) : MQTTPacket
