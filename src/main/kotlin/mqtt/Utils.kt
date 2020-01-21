package mqtt

import mqtt.packets.MQTTPublish
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset

fun OutputStream.encodeVariableByteInteger(value: UInt) {
    var x = value
    do {
        var encodedByte = x.rem(128u)
        x /= 128u
        if (x > 0u) {
            encodedByte = encodedByte or 128u
        }
        write(encodedByte.toInt())
    } while (x > 0u)
}

fun InputStream.decodeVariableByteInteger(): UInt {
    var multiplier = 1u
    var value = 0u
    do {
        val encodedByte = read().toUInt()
        value += (encodedByte and 127u) * multiplier
        if (multiplier > 128u * 128u * 128u) {
            throw Exception("Malformed Variable Byte Integer")
        }
        multiplier *= 128u
    } while ((encodedByte and 128u) != 0u)
    return value
}

fun MQTTPublish.messageExpiryIntervalExpired(): Boolean {
    val expiry = properties.messageExpiryInterval?.toLong() ?: (Long.MAX_VALUE / 1000)
    return ((expiry * 1000) + timestamp) < System.currentTimeMillis()
}

fun ByteArray.validatePayloadFormat(indicator: UInt): Boolean {
    if (indicator == 1u) {
        return try {
            Charset.forName("UTF-8").newDecoder().decode(ByteBuffer.wrap(this))
            true
        } catch (e: CharacterCodingException) {
            false
        }
    }
    return true
}
