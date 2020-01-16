package mqtt

import mqtt.packets.*
import java.io.DataInputStream
import java.io.InputStream

class MQTTInputStream(inputStream: InputStream) : DataInputStream(inputStream) {

    @ExperimentalUnsignedTypes
    fun readPacket(): MQTTPacket {
        val byte1 = read()
        val mqttControlPacketType = (byte1 shr 4) and 0b1111
        val flags = byte1 and 0b1111

        val type = MQTTControlPacketType.valueOf(mqttControlPacketType)!!

        val remainingLength = read()
        val packet = ByteArray(remainingLength)
        readFully(packet, 0, remainingLength)
        return parseMQTTPacket(type, flags, packet)
    }

    private fun parseMQTTPacket(type: MQTTControlPacketType, flags: Int, data: ByteArray): MQTTPacket {
        return when (type) {
            MQTTControlPacketType.CONNECT -> MQTTConnect(flags, data)
            MQTTControlPacketType.Reserved -> TODO("Throw exception malformed packet")
            MQTTControlPacketType.CONNACK -> MQTTConnack(flags, data)
            MQTTControlPacketType.PUBLISH -> MQTTPublish(flags, data)
            MQTTControlPacketType.PUBACK -> MQTTPuback(flags, data)
            MQTTControlPacketType.PUBREC -> MQTTPubrec(flags, data)
            MQTTControlPacketType.PUBREL -> MQTTPubrel(flags, data)
            MQTTControlPacketType.PUBCOMP -> MQTTPubcomp(flags, data)
            MQTTControlPacketType.SUBSCRIBE -> MQTTSubscribe(flags, data)
            MQTTControlPacketType.SUBACK -> MQTTSuback(flags, data)
            MQTTControlPacketType.UNSUBSCRIBE -> MQTTUnsubscribe(flags, data)
            MQTTControlPacketType.UNSUBACK -> MQTTUnsuback(flags, data)
            MQTTControlPacketType.PINGREQ -> MQTTPingreq(flags, data)
            MQTTControlPacketType.PINGRESP -> MQTTPingresp(flags, data)
            MQTTControlPacketType.DISCONNECT -> MQTTDisconnect(flags, data)
            MQTTControlPacketType.AUTH -> MQTTAuth(flags, data)
        }
    }
}
