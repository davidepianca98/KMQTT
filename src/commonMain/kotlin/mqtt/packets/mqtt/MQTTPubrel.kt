package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

abstract class MQTTPubrel(val packetId: UInt) : MQTTPacket
