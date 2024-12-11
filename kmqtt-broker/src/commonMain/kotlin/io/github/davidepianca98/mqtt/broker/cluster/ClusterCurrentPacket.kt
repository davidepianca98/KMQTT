package io.github.davidepianca98.mqtt.broker.cluster

import io.github.davidepianca98.mqtt.broker.cluster.packets.AddSessionPacket
import io.github.davidepianca98.mqtt.broker.cluster.packets.AddSubscriptionPacket
import io.github.davidepianca98.mqtt.broker.cluster.packets.ClusterPacket
import io.github.davidepianca98.mqtt.broker.cluster.packets.PublishPacket
import io.github.davidepianca98.mqtt.broker.cluster.packets.RemoveSubscriptionPacket
import io.github.davidepianca98.mqtt.broker.cluster.packets.SessionTakenOverPacket
import io.github.davidepianca98.mqtt.broker.cluster.packets.SessionUpdatePacket
import io.github.davidepianca98.mqtt.broker.cluster.packets.SetRetainedPacket
import io.github.davidepianca98.socket.streams.DynamicByteBuffer
import io.github.davidepianca98.socket.streams.EOFException
import io.github.davidepianca98.socket.streams.decodeVariableByteInteger

internal class ClusterCurrentPacket {

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
