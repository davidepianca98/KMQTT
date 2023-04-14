package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

public abstract class MQTTSuback(public val packetIdentifier: UInt) : MQTTPacket
