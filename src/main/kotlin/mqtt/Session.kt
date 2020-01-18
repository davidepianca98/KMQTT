package mqtt

import mqtt.packets.MQTTConnect

class Session(packet: MQTTConnect) {

    val clientId = packet.clientID
    val keepAlive = packet.keepAlive

    // TODO publish will when:
    //  An I/O error or network failure detected by the Server.
    //  The Client fails to communicate within the Keep Alive time.
    //  The Client closes the Network Connection without first sending a DISCONNECT packet with a Reason Code 0x00 (Normal disconnection).
    //  The Server closes the Network Connection without first receiving a DISCONNECT packet with a Reason Code 0x00 (Normal disconnection).

    // TODO must be removed from the session if disconnect reson success or will message published
    val will = if (packet.connectFlags.willFlag)
        Will(
            packet.connectFlags.willRetain,
            packet.connectFlags.willQos,
            packet.willTopic!!,
            packet.willPayload!!,
            packet.willProperties!!.willDelayInterval
                ?: 0, // TODO publish will after this interval or when the session ends, first to come
            packet.willProperties.payloadFormatIndicator ?: 0, // TODO if 1 validate willpayload is utf-8
            packet.willProperties.messageExpiryInterval, // TODO lifetime of will message as publication expiry interval when sending
            packet.willProperties.contentType, // TODO send as content type if present
            packet.willProperties.responseTopic, // TODO send as response topic if present
            packet.willProperties.correlationData, // TODO send as correlation data if present
            packet.willProperties.userProperty // TODO send as user properties maintaining order
        )
    else
        null

    // TODO if 0 delete session on disconnection else delete after timeout, if 0xFFFFFFFF never delete
    val sessionExpiryInterval = packet.properties.sessionExpiryInterval ?: 0
    // TODO handle flow control as explained in section 4.9
    val receiveMaximum = packet.properties.receiveMaximum ?: 65535
    // TODO don't send packets larger than this, if null no limit
    val maximumPacketSize = packet.properties.maximumPacketSize
    // TODO if 0 don't send topic alias, otherwise maximum number of aliases
    val topicAliasMaximum = packet.properties.topicAliasMaximum ?: 0
    // TODO if different from 0 or 1 protocol error, if 1 may return response information in connack
    val requestResponseInformation = packet.properties.requestResponseInformation ?: 0
    // TODO if different from 0 or 1 protocol error, if 0 may send a reson string or user properties in connack or disconnect, but no reason string or user properties in any other packet than publish, connack, disconnect
    val requestProblemInformation = packet.properties.requestProblemInformation ?: 1
    val userProperties = packet.properties.userProperty


    class Will(
        val retain: Boolean,
        val qos: Int,
        val topic: String,
        val payload: ByteArray,
        val willDelayInterval: Int,
        val payloadFormatIndicator: Int,
        val messageExpiryInterval: Int?,
        val contentType: String?,
        val responseTopic: String?,
        val correlationData: ByteArray?,
        val userProperty: Map<String, String>
    )

    fun clean() {

    }
}

/*
The Clients subscriptions, including any Subscription Identifiers.

QoS 1 and QoS 2 messages which have been sent to the Client, but have not been completely acknowledged.

QoS 1 and QoS 2 messages pending transmission to the Client and OPTIONALLY QoS 0 messages pending transmission to the Client.

QoS 2 messages which have been received from the Client, but have not been completely acknowledged.The Will Message and the Will Delay Interval

If the Session is currently not connected, the time at which the Session will end and Session State will be discarded.
 */
