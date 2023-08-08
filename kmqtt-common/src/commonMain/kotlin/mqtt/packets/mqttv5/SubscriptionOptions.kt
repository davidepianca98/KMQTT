package mqtt.packets.mqttv5

import mqtt.MQTTException
import mqtt.packets.Qos

public data class SubscriptionOptions(
    public val qos: Qos = Qos.AT_MOST_ONCE,
    public val noLocal: Boolean = false,
    public val retainedAsPublished: Boolean = false,
    public val retainHandling: UInt = 0u
) {
    public fun toByte(): UInt {
        if (retainHandling !in 0u..2u)
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
        val optionsByte = 0 or
                ((retainHandling.toInt() shl 4) and 0x30) or
                (((if (retainedAsPublished) 1 else 0) shl 3) and 0x8) or
                (((if (noLocal) 1 else 0) shl 2) and 0x4) or
                (qos.value and 0x3)
        return optionsByte.toUInt()
    }
}
