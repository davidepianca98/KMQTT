package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

public abstract class MQTTPubrel(public val packetId: UInt) : MQTTPacket
