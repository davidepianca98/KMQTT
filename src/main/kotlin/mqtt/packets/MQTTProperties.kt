package mqtt.packets

data class MQTTProperties(
    var payloadFormatIndicator: Int? = null,
    var messageExpiryInterval: Int? = null,
    var contentType: String? = null,
    var responseTopic: String? = null,
    var correlationData: ByteArray? = null,
    var subscriptionIdentifier: Int? = null,
    var sessionExpiryInterval: Int? = null,
    var assignedClientIdentifier: String? = null,
    var serverKeepAlive: Int? = null,
    var authenticationMethod: String? = null,
    var authenticationData: ByteArray? = null,
    var requestInformation: Int? = null,
    var willDelayInterval: Int? = null,
    var requestResponseInformation: Int? = null,
    var responseInformation: String? = null,
    var serverReference: String? = null,
    var reasonString: String? = null,
    var receiveMaximum: Int? = null,
    var topicAliasMaximum: Int? = null,
    var topicAlias: Int? = null,
    var maximumQos: Int? = null,
    var retainAvailable: Int? = null,
    val userProperty: MutableMap<String, String> = mutableMapOf(),
    var maximumPacketSize: Int? = null,
    var wildcardSubscriptionAvailable: Int? = null,
    var subscriptionIdentifierAvailable: Int? = null,
    var sharedSubscriptionAvailable: Int? = null
) {
    fun addUserProperty(property: Pair<String, String>) {
        userProperty[property.first] = property.second
    }
}
