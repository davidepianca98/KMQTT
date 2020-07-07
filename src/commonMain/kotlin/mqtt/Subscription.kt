package mqtt

import mqtt.packets.mqttv5.SubscriptionOptions

class Subscription(
    val topicFilter: String,
    val options: SubscriptionOptions = SubscriptionOptions(),
    val subscriptionIdentifier: UInt? = null
) {
    val matchTopicFilter = topicFilter.getSharedTopicFilter() ?: topicFilter
    val shareName = topicFilter.getSharedTopicShareName()
    var timestampShareSent: Long = 0

    fun isShared(): Boolean = topicFilter.isSharedTopicFilter()
}
