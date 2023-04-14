package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

public abstract class MQTTPuback(public val packetId: UInt) : MQTTPacket
