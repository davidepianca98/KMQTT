package mqtt

import mqtt.packets.MQTTSubscribe

class Subscription( // TODO shared subscription
    val topicFilter: String,
    val options: MQTTSubscribe.Companion.SubscriptionOptions,
    val subscriptionIdentifier: UInt?
) {
    fun isShared(): Boolean {
        val split = topicFilter.split("/")
        if (split.size != 3)
            return false
        if (split[0] == "\$share/" && split[1].isNotEmpty() && !split[1].contains("/") && !split[1].contains("+") && !split[1].contains(
                "#"
            ) && split[2].isValidTopic()
        )
            return true
        return false
    }
}
