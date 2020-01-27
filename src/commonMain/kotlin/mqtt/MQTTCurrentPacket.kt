package mqtt

import mqtt.packets.*
import socket.streams.DynamicByteBuffer
import socket.streams.EOFException
import socket.streams.decodeVariableByteInteger

class MQTTCurrentPacket(private val maximumPacketSize: UInt) {

    private val currentReceivedData = DynamicByteBuffer()

    fun addData(data: UByteArray): MQTTPacket? { // TODO optimize packet reception and handling
        return try {
            currentReceivedData.write(data)
            readPacket()
        } catch (e: EOFException) {
            currentReceivedData.clearReadCounter()
            null
        }
    }

    private fun readPacket(): MQTTPacket {
        val byte1 = currentReceivedData.read()
        val mqttControlPacketType = (byte1.toInt() shr 4) and 0b1111
        val flags = byte1 and 0b1111u

        val type = MQTTControlPacketType.valueOf(mqttControlPacketType)!!

        val remainingLength = currentReceivedData.decodeVariableByteInteger().toInt()

        if (remainingLength + 2 > maximumPacketSize.toInt())
            throw MQTTException(ReasonCode.PACKET_TOO_LARGE)

        val packetData = currentReceivedData.readBytes(remainingLength)
        val packet = parseMQTTPacket(type, flags.toInt(), packetData)
        currentReceivedData.shift()
        return packet
    }

    private fun parseMQTTPacket(type: MQTTControlPacketType, flags: Int, data: UByteArray): MQTTPacket {
        return when (type) {
            MQTTControlPacketType.CONNECT -> MQTTConnect.fromByteArray(flags, data)
            MQTTControlPacketType.Reserved -> throw MQTTException(
                ReasonCode.MALFORMED_PACKET
            )
            MQTTControlPacketType.CONNACK -> MQTTConnack.fromByteArray(flags, data)
            MQTTControlPacketType.PUBLISH -> MQTTPublish.fromByteArray(flags, data)
            MQTTControlPacketType.PUBACK -> MQTTPuback.fromByteArray(flags, data)
            MQTTControlPacketType.PUBREC -> MQTTPubrec.fromByteArray(flags, data)
            MQTTControlPacketType.PUBREL -> MQTTPubrel.fromByteArray(flags, data)
            MQTTControlPacketType.PUBCOMP -> MQTTPubcomp.fromByteArray(flags, data)
            MQTTControlPacketType.SUBSCRIBE -> MQTTSubscribe.fromByteArray(flags, data)
            MQTTControlPacketType.SUBACK -> MQTTSuback.fromByteArray(flags, data)
            MQTTControlPacketType.UNSUBSCRIBE -> MQTTUnsubscribe.fromByteArray(flags, data)
            MQTTControlPacketType.UNSUBACK -> MQTTUnsuback.fromByteArray(flags, data)
            MQTTControlPacketType.PINGREQ -> MQTTPingreq.fromByteArray(flags, data)
            MQTTControlPacketType.PINGRESP -> MQTTPingresp.fromByteArray(flags, data)
            MQTTControlPacketType.DISCONNECT -> MQTTDisconnect.fromByteArray(flags, data)
            MQTTControlPacketType.AUTH -> MQTTAuth.fromByteArray(flags, data)
        }
    }

}
