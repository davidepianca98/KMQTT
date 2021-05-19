package mqtt.broker.cluster

import mqtt.broker.cluster.packets.*
import socket.streams.DynamicByteBuffer
import socket.streams.EOFException
import socket.streams.decodeVariableByteInteger

class ClusterCurrentPacket {

    private val currentReceivedData = DynamicByteBuffer()

    fun addData(data: UByteArray): List<ClusterPacket> {
        val packets = mutableListOf<ClusterPacket>()
        currentReceivedData.write(data)
        try {
            while (true) {
                packets += readPacket()!!
            }
        } catch (e: EOFException) {
            currentReceivedData.clearReadCounter()
        } catch (e: NullPointerException) {
            println("Unknown cluster packet")
        }
        return packets
    }

    private fun readPacket(): ClusterPacket? {
        val type = currentReceivedData.read()

        val optionsLength = currentReceivedData.decodeVariableByteInteger()

        @Suppress("UNUSED_VARIABLE")
        val optionsData = currentReceivedData.readBytes(optionsLength.toInt())

        val remainingLength = currentReceivedData.decodeVariableByteInteger().toInt()
        val packetData = currentReceivedData.readBytes(remainingLength)

        val packet = parseClusterPacket(type, packetData)
        currentReceivedData.shift()
        return packet
    }

    private fun parseClusterPacket(type: UByte, packetData: UByteArray): ClusterPacket? {
        return when (type.toInt()) {
            0 -> SetRetainedPacket.fromByteArray(packetData)
            1 -> AddSubscriptionPacket.fromByteArray(packetData)
            2 -> RemoveSubscriptionPacket.fromByteArray(packetData)
            3 -> PublishPacket.fromByteArray(packetData)
            4 -> AddSessionPacket.fromByteArray(packetData)
            5 -> SessionUpdatePacket.fromByteArray(packetData)
            6 -> SessionTakenOverPacket.fromByteArray(packetData)
            else -> null
        }
    }
}
