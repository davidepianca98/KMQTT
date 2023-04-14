package mqtt.packets.mqttv5

public class MQTT5Properties(
    public var payloadFormatIndicator: UInt? = null,
    public var messageExpiryInterval: UInt? = null,
    public var contentType: String? = null,
    public var responseTopic: String? = null,
    public var correlationData: UByteArray? = null,
    public val subscriptionIdentifier: MutableList<UInt> = mutableListOf(),
    public var sessionExpiryInterval: UInt? = null,
    public var assignedClientIdentifier: String? = null,
    public var serverKeepAlive: UInt? = null,
    public var authenticationMethod: String? = null,
    public var authenticationData: UByteArray? = null,
    public var requestProblemInformation: UInt? = null,
    public var willDelayInterval: UInt? = null,
    public var requestResponseInformation: UInt? = null,
    public var responseInformation: String? = null,
    public var serverReference: String? = null,
    public var reasonString: String? = null,
    public var receiveMaximum: UInt? = null,
    public var topicAliasMaximum: UInt? = null,
    public var topicAlias: UInt? = null,
    public var maximumQos: UInt? = null,
    public var retainAvailable: UInt? = null,
    public val userProperty: MutableList<Pair<String, String>> = mutableListOf(),
    public var maximumPacketSize: UInt? = null,
    public var wildcardSubscriptionAvailable: UInt? = null,
    public var subscriptionIdentifierAvailable: UInt? = null,
    public var sharedSubscriptionAvailable: UInt? = null
) {
    public fun addUserProperty(property: Pair<String, String>) {
        userProperty += property
    }
}
