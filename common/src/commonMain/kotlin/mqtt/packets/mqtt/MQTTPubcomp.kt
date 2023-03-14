package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

abstract class MQTTPubcomp(val packetId: UInt) : MQTTPacket
