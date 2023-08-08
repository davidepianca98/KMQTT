package mqtt

import mqtt.packets.mqttv5.SubscriptionOptions

public data class Subscription(
    public val topicFilter: String,
    public val options: SubscriptionOptions = SubscriptionOptions(),
    public val subscriptionIdentifier: UInt? = null
) {
    public val matchTopicFilter: String = topicFilter.getSharedTopicFilter() ?: topicFilter
    public val shareName: String? = topicFilter.getSharedTopicShareName()
    public var timestampShareSent: Long = 0

    public fun isShared(): Boolean = topicFilter.isSharedTopicFilter()
}
