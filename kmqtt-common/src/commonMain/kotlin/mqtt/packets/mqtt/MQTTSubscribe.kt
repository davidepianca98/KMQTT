package mqtt.packets.mqtt

import mqtt.Subscription
import mqtt.packets.MQTTPacket

public abstract class MQTTSubscribe(
    public val packetIdentifier: UInt,
    public val subscriptions: List<Subscription>
) : MQTTPacket
