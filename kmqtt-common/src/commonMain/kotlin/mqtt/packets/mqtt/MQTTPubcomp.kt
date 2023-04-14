package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

public abstract class MQTTPubcomp(public val packetId: UInt) : MQTTPacket
