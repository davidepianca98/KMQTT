package mqtt

import ClientHandler
import mqtt.packets.MQTTConnect

class Session(packet: MQTTConnect, var clientHandler: ClientHandler) {

    var connected = false

    var clientId = packet.clientID
    var keepAlive = packet.keepAlive

    // TODO publish will when:
    //  An I/O error or network failure detected by the Server.
    //  The Client fails to communicate within the Keep Alive time.
    //  The Client closes the Network Connection without first sending a DISCONNECT packet with a Reason Code 0x00 (Normal disconnection).
    //  The Server closes the Network Connection without first receiving a DISCONNECT packet with a Reason Code 0x00 (Normal disconnection).

    // TODO must be removed from the session if disconnect reason success or will message published
    var will = buildWill(packet)

    // TODO if 0 delete session on disconnection else delete after timeout, if 0xFFFFFFFF never delete
    var sessionExpiryInterval = packet.properties.sessionExpiryInterval ?: 0u
    // TODO handle flow control as explained in section 4.9
    var receiveMaximum = packet.properties.receiveMaximum ?: 65535u
    // TODO don't send packets larger than this, if null no limit
    var maximumPacketSize = packet.properties.maximumPacketSize
    // TODO if 0 don't send topic alias, otherwise maximum number of aliases
    var topicAliasMaximum = packet.properties.topicAliasMaximum ?: 0u
    // TODO if different from 0 or 1 protocol error, if 1 may return response information in connack
    var requestResponseInformation = packet.properties.requestResponseInformation ?: 0u
    // TODO if different from 0 or 1 protocol error, if 0 may send a reson string or user properties in connack or disconnect, but no reason string or user properties in any other packet than publish, connack, disconnect
    var requestProblemInformation = packet.properties.requestProblemInformation ?: 1u
    var userProperties = packet.properties.userProperty


    class Will(
        val retain: Boolean,
        val qos: Int,
        val topic: String,
        val payload: ByteArray,
        val willDelayInterval: UInt,
        val payloadFormatIndicator: UInt,
        val messageExpiryInterval: UInt?,
        val contentType: String?,
        val responseTopic: String?,
        val correlationData: ByteArray?,
        val userProperty: Map<String, String>
    )

    private fun buildWill(packet: MQTTConnect): Will? {
        return if (packet.connectFlags.willFlag)
            Will(
                packet.connectFlags.willRetain,
                packet.connectFlags.willQos,
                packet.willTopic!!,
                packet.willPayload!!,
                packet.willProperties!!.willDelayInterval
                    ?: 0u, // TODO publish will after this interval or when the session ends, first to come
                packet.willProperties.payloadFormatIndicator ?: 0u, // TODO if 1 validate willpayload is utf-8
                packet.willProperties.messageExpiryInterval, // TODO lifetime of will message as publication expiry interval when sending
                packet.willProperties.contentType, // TODO send as content type if present
                packet.willProperties.responseTopic, // TODO send as response topic if present
                packet.willProperties.correlationData, // TODO send as correlation data if present
                packet.willProperties.userProperty // TODO send as user properties maintaining order
            )
        else
            null
    }

    fun update(packet: MQTTConnect) {
        clientId = packet.clientID
        keepAlive = packet.keepAlive
        will = buildWill(packet)

        sessionExpiryInterval = packet.properties.sessionExpiryInterval ?: 0u
        receiveMaximum = packet.properties.receiveMaximum ?: 65535u
        maximumPacketSize = packet.properties.maximumPacketSize
        topicAliasMaximum = packet.properties.topicAliasMaximum ?: 0u
        requestResponseInformation = packet.properties.requestResponseInformation ?: 0u
        requestProblemInformation = packet.properties.requestProblemInformation ?: 1u
        userProperties = packet.properties.userProperty
    }

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
