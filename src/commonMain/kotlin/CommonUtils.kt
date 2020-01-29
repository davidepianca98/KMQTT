import kotlinx.coroutines.CoroutineScope
import mqtt.MQTTException
import mqtt.packets.mqttv5.MQTTPublish
import mqtt.packets.mqttv5.ReasonCode
import kotlin.random.Random

expect fun currentTimeMillis(): Long

expect fun runCoroutine(block: suspend CoroutineScope.() -> Unit)

fun generateRandomClientId(): String {
    val length = 30
    val buffer = StringBuilder(length)
    for (i in 0 until length) {
        buffer.append(Random.Default.nextInt(97, 122).toChar())
    }
    return buffer.toString()
}

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

fun UByteArray.toHexString() = joinToString("") { it.toString(16).padStart(2, '0') }
