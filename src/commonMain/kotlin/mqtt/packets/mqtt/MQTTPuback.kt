package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

abstract class MQTTPuback(val packetId: UInt) : MQTTPacket
