package mqtt.packets.mqtt

import mqtt.Subscription
import mqtt.packets.MQTTPacket

abstract class MQTTSubscribe(
    val packetIdentifier: UInt,
    val subscriptions: List<Subscription>
) : MQTTPacket
