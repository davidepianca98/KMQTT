package mqtt.packets.mqtt

import mqtt.packets.MQTTPacket

public abstract class MQTTPubrec(public val packetId: UInt) : MQTTPacket
