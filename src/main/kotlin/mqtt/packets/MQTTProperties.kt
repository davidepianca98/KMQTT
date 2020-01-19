package mqtt.packets

class MQTTProperties(
    var payloadFormatIndicator: UInt? = null,
    var messageExpiryInterval: UInt? = null,
    var contentType: String? = null,
    var responseTopic: String? = null,
    var correlationData: ByteArray? = null,
    var subscriptionIdentifier: MutableList<UInt> = mutableListOf(),
    var sessionExpiryInterval: UInt? = null,
    var assignedClientIdentifier: String? = null,
    var serverKeepAlive: UInt? = null,
    var authenticationMethod: String? = null,
    var authenticationData: ByteArray? = null,
    var requestProblemInformation: UInt? = null,
    var willDelayInterval: UInt? = null,
    var requestResponseInformation: UInt? = null,
    var responseInformation: String? = null,
    var serverReference: String? = null,
    var reasonString: String? = null,
    var receiveMaximum: UInt? = null,
    var topicAliasMaximum: UInt? = null,
    var topicAlias: UInt? = null,
    var maximumQos: UInt? = null,
    var retainAvailable: UInt? = null,
    val userProperty: MutableMap<String, String> = mutableMapOf(), // TODO shouldn't be map because the key can appear multiple times
    var maximumPacketSize: UInt? = null,
    var wildcardSubscriptionAvailable: UInt? = null,
    var subscriptionIdentifierAvailable: UInt? = null,
    var sharedSubscriptionAvailable: UInt? = null
) {
    fun addUserProperty(property: Pair<String, String>) {
        userProperty[property.first] = property.second
    }
}
