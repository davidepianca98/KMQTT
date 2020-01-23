package mqtt

import mqtt.packets.MQTTPublish
import mqtt.packets.ReasonCode
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
    val expiry = properties.messageExpiryInterval?.toLong() ?: ((Long.MAX_VALUE / 1000) - timestamp)
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

fun String.validateUTF8String() { // Taken from Paho MQTT Java
    this.forEachIndexed { i, character ->
        var isBad = false
        if (Character.isHighSurrogate(character)) {
            if (i + 1 == this.length) {
                isBad = true
            } else {
                val c2 = this[i + 1]
                if (!Character.isLowSurrogate(c2)) {
                    isBad = true
                } else {
                    val ch = ((character.toInt() and 0x3ff) shl 10) or (c2.toInt() and 0x3ff)
                    if (ch and 0xffff == 0xffff || ch and 0xffff == 0xfffe) {
                        isBad = true
                    }
                }
            }
        } else {
            if (Character.isISOControl(character) || Character.isLowSurrogate(character)) {
                isBad = true
            } else if (character.toInt() >= 0xfdd0 && (character.toInt() <= 0xfddf || character.toInt() >= 0xfffe)) {
                isBad = true
            }
        }
        if (isBad)
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
    }
}
