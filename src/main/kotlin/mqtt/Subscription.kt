package mqtt

class Subscription( // TODO shared subscription
    val topicName: String,
    val qos: Int,
    val subscriptionIdentifier: UInt?
)
