package mqtt

import mqtt.packets.*
import socket.streams.InputStream
import socket.streams.decodeVariableByteInteger

class MQTTInputStream(private val inputStream: InputStream, private val maximumPacketSize: UInt? = null) : InputStream {

    suspend fun readPacket(): MQTTPacket {
        val byte1 = read()
        val mqttControlPacketType = (byte1.toInt() shr 4) and 0b1111
        val flags = byte1 and 0b1111u

        val type = MQTTControlPacketType.valueOf(mqttControlPacketType)!!

        val remainingLength = decodeVariableByteInteger().toInt()

        maximumPacketSize?.let {
            if (remainingLength + 2 > it.toInt())
                throw MQTTException(ReasonCode.PACKET_TOO_LARGE)
        }

        val packet = readBytes(remainingLength)
        return parseMQTTPacket(type, flags.toInt(), packet)
    }

    private suspend fun parseMQTTPacket(type: MQTTControlPacketType, flags: Int, data: UByteArray): MQTTPacket {
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

    override suspend fun read(): UByte {
        return inputStream.read()
    }

    override suspend fun readBytes(length: Int): UByteArray {
        return inputStream.readBytes(length)
    }
}
