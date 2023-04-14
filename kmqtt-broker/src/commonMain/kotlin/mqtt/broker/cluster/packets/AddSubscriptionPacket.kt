package mqtt.broker.cluster.packets

import mqtt.Subscription
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream
import socket.streams.encodeVariableByteInteger

internal class AddSubscriptionPacket(val clientId: String, val subscription: Subscription) : ClusterPacket {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        // Type
        outStream.write(1.toUByte())

        // Options length
        outStream.encodeVariableByteInteger(0u)


        val dataOutStream = ByteArrayOutputStream()
        dataOutStream.writeUTF8String(clientId)

        dataOutStream.writeUTF8String(subscription.topicFilter)
        dataOutStream.writeByte(subscription.options.qos.value.toUInt())
        dataOutStream.writeByte(if (subscription.options.noLocal) 1u else 0u)
        dataOutStream.writeByte(if (subscription.options.retainedAsPublished) 1u else 0u)
        dataOutStream.writeByte(subscription.options.retainHandling.toUInt())
        dataOutStream.write4BytesInt(subscription.subscriptionIdentifier?.toUInt() ?: 0u)

        val data = dataOutStream.toByteArray()

        outStream.encodeVariableByteInteger(data.size.toUInt())
        outStream.write(data)
        return outStream.toByteArray()
    }

    companion object : ClusterPacketDeserializer {

        override fun fromByteArray(data: UByteArray): AddSubscriptionPacket {
            val inStream = ByteArrayInputStream(data)

            val clientId = inStream.readUTF8String()

            val topicFilter = inStream.readUTF8String()
            val qos = inStream.read().toInt()
            val noLocal = inStream.read().toInt() == 1
            val retainedAsPublished = inStream.read().toInt() == 1
            val retainHandling = inStream.read().toUInt()

            val subIdVal = inStream.read4BytesInt()
            val subscriptionIdentifier = if (subIdVal == 0u) null else subIdVal

            return AddSubscriptionPacket(
                clientId, Subscription(
                    topicFilter, SubscriptionOptions(
                        Qos.valueOf(qos)!!, noLocal, retainedAsPublished, retainHandling
                    ), subscriptionIdentifier
                )
            )
        }

    }
}
