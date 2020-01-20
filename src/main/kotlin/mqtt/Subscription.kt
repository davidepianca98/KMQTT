package mqtt

import mqtt.packets.Qos

class Subscription( // TODO shared subscription
    val topicName: String,
    val qos: Qos,
    val subscriptionIdentifier: UInt?
)
