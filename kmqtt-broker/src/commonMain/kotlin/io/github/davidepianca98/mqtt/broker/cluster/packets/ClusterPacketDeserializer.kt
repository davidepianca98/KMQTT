package io.github.davidepianca98.mqtt.broker.cluster.packets

import io.github.davidepianca98.mqtt.MQTTException
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.validateUTF8String

internal interface ClusterPacketDeserializer {

    fun fromByteArray(data: UByteArray): ClusterPacket

    fun ByteArrayInputStream.read4BytesInt(): UInt {
        return (read().toUInt() shl 24) or (read().toUInt() shl 16) or (read().toUInt() shl 8) or read().toUInt()
    }

    fun ByteArrayInputStream.read2BytesInt(): UInt {
        return (read().toUInt() shl 8) or read().toUInt()
    }

    fun ByteArrayInputStream.readByte(): UInt {
        return read().toUInt()
    }

    fun ByteArrayInputStream.readUTF8String(): String {
        val length = read2BytesInt().toInt()
        val string = readBytes(length).toByteArray().decodeToString()
        if (!string.validateUTF8String())
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
        return string
    }
}
