import mqtt.packets.mqttv5.MQTTPublish
import kotlin.random.Random

expect fun currentTimeMillis(): Long

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
        return this.toByteArray().decodeToString().validateUTF8String()
    }
    return true
}

fun String.validateUTF8String(): Boolean {
    this.forEachIndexed { index, character ->
        if (character == '\u0000')
            return false
        if (character == '\uFFFD')
            return false
        if (character in '\uD800'..'\uDFFF') {
            this.getOrNull(index + 1)?.let {
                if (it !in '\uDC00'..'\uDFFF')
                    return false
            } ?: return false
        }
    }
    return true
}

fun UByteArray.toHexString() = joinToString("") { it.toString(16).padStart(2, '0') }
