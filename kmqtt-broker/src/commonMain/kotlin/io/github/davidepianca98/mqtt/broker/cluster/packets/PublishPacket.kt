package io.github.davidepianca98.mqtt.broker.cluster.packets

import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPublish
import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream
import io.github.davidepianca98.socket.streams.encodeVariableByteInteger

internal class PublishPacket(val packet: MQTTPublish) : ClusterPacket {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        // Type
        outStream.write(3.toUByte())

        // Options length
        outStream.encodeVariableByteInteger(0u)

        val publish = packet.toByteArray()
        // Data length
        outStream.encodeVariableByteInteger(publish.size.toUInt())

        // Data
        outStream.write(publish)

        return outStream.toByteArray()
    }

    companion object : ClusterPacketDeserializer {

        override fun fromByteArray(data: UByteArray): PublishPacket {
            val inStream = ByteArrayInputStream(data)

            val packetData = inStream.readRemaining()

            return PublishPacket(MQTTPublish.fromByteArray(5, packetData)) // TODO version 4 also
        }

    }
}
