package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

public abstract class MQTTUnsuback(public val packetIdentifier: UInt) : MQTTPacket
