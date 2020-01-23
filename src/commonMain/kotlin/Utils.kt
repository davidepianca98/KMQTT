import mqtt.MQTTException
import mqtt.packets.MQTTPublish
import mqtt.packets.ReasonCode
import mqtt.streams.ByteArrayInputStream
import mqtt.streams.ByteArrayOutputStream

fun ByteArrayOutputStream.encodeVariableByteInteger(value: UInt): Int {
    var length = 0
    var x = value
    do {
        var encodedByte = x.rem(128u)
        x /= 128u
        if (x > 0u) {
            encodedByte = encodedByte or 128u
        }
        write(encodedByte)
        length++
    } while (x > 0u)
    return length
}

fun ByteArrayInputStream.decodeVariableByteInteger(): UInt {
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

expect fun currentTimeMillis(): Long

fun MQTTPublish.messageExpiryIntervalExpired(): Boolean {
    val expiry = properties.messageExpiryInterval?.toLong() ?: ((Long.MAX_VALUE / 1000) - timestamp)
    return ((expiry * 1000) + timestamp) < currentTimeMillis()
}

fun UByteArray.validatePayloadFormat(indicator: UInt): Boolean {
    if (indicator == 1u) {
        return try {
            this.toByteArray().decodeToString().validateUTF8String()
            true
        } catch (e: MQTTException) {
            false
        }
    }
    return true
}

fun String.validateUTF8String() { // Taken from Paho MQTT Java
    this.forEachIndexed { i, character ->
        var isBad = false
        if (character in '\uD800'..'\uDBFF') {
            if (i + 1 == this.length) {
                isBad = true
            } else {
                val c2 = this[i + 1]
                if (c2 !in '\uDC00'..'\uDFFF') {
                    isBad = true
                } else {
                    val ch = ((character.toInt() and 0x3ff) shl 10) or (c2.toInt() and 0x3ff)
                    if (ch and 0xffff == 0xffff || ch and 0xffff == 0xfffe) {
                        isBad = true
                    }
                }
            }
        } else {
            if ((character.toInt() <= 0x9F &&
                        (character.toInt() >= 0x7F || character.toInt() ushr 5 == 0)) || character in '\uDC00'..'\uDFFF'
            ) {
                isBad = true
            } else if (character.toInt() >= 0xfdd0 && (character.toInt() <= 0xfddf || character.toInt() >= 0xfffe)) {
                isBad = true
            }
        }
        if (isBad)
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
    }
}
