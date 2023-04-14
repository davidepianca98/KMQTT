package mqtt.packets.mqttv4

import mqtt.MQTTException
import mqtt.packets.ConnectFlags
import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTConnect
import mqtt.packets.mqttv5.ReasonCode
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream


public class MQTT4Connect(
    protocolName: String,
    protocolVersion: Int,
    connectFlags: ConnectFlags,
    keepAlive: Int,
    clientID: String = "",
    willTopic: String? = null,
    willPayload: UByteArray? = null,
    userName: String? = null,
    password: UByteArray? = null
) : MQTTConnect(
    protocolName,
    protocolVersion,
    connectFlags,
    keepAlive,
    clientID,
    willTopic,
    willPayload,
    userName,
    password
), MQTT4Packet {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()
        outStream.writeUTF8String("MQTT")
        outStream.writeByte(4u)
        outStream.writeByte(connectFlags.toByte())
        outStream.write2BytesInt(keepAlive.toUInt())

        // Payload
        outStream.writeUTF8String(clientID)
        try {
            if (connectFlags.willFlag) {
                outStream.writeUTF8String(willTopic!!)
                outStream.writeBinaryData(willPayload!!)
            }
        } catch (e: NullPointerException) {
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
        }

        try {
            if (connectFlags.userNameFlag) {
                outStream.writeUTF8String(userName!!)
            }
        } catch (e: NullPointerException) {
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
        }

        try {
            if (connectFlags.passwordFlag) {
                outStream.writeBinaryData(password!!)
            }
        } catch (e: NullPointerException) {
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
        }

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.CONNECT, 0)
    }

    public companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Connect {
            val inStream = ByteArrayInputStream(data)
            val protocolName = inStream.readUTF8String()
            if (protocolName != "MQTT")
                throw MQTTException(ReasonCode.UNSUPPORTED_PROTOCOL_VERSION)
            val protocolVersion = inStream.read().toInt()
            if (protocolVersion != 4)
                throw MQTTException(ReasonCode.UNSUPPORTED_PROTOCOL_VERSION)

            val connectFlags = ConnectFlags.connectFlags(inStream.read().toInt())
            val keepAlive = inStream.read2BytesInt()

            // Payload
            val clientID = inStream.readUTF8String()

            val willTopic = if (connectFlags.willFlag) inStream.readUTF8String() else null
            val willPayload = if (connectFlags.willFlag) inStream.readBinaryData() else null
            val userName = if (connectFlags.userNameFlag) inStream.readUTF8String() else null
            val password = if (connectFlags.passwordFlag) inStream.readBinaryData() else null

            return MQTT4Connect(
                protocolName,
                protocolVersion,
                connectFlags,
                keepAlive.toInt(),
                clientID,
                willTopic,
                willPayload,
                userName,
                password
            )
        }

    }
}
