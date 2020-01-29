package mqtt

import mqtt.packets.mqttv5.MQTTSubscribe

class Subscription(
    val topicFilter: String,
    val options: MQTTSubscribe.Companion.SubscriptionOptions,
    val subscriptionIdentifier: UInt?
) {
    val matchTopicFilter = topicFilter.getSharedTopicFilter() ?: topicFilter
    val shareName = topicFilter.getSharedTopicShareName()
    var timestampShareSent: Long = 0

    fun isShared(): Boolean = topicFilter.isSharedTopicFilter()
}
