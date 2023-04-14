package mqtt.packets

import mqtt.MQTTException
import mqtt.packets.mqttv5.ReasonCode

public data class ConnectFlags(
    val userNameFlag: Boolean,
    val passwordFlag: Boolean,
    val willRetain: Boolean,
    val willQos: Qos,
    val willFlag: Boolean,
    val cleanStart: Boolean,
    val reserved: Boolean
) {
    public fun toByte(): UInt {
        val flags = (((if (userNameFlag) 1 else 0) shl 7) and 0x80) or
                (((if (passwordFlag) 1 else 0) shl 6) and 0x40) or
                (((if (willRetain) 1 else 0) shl 5) and 0x20) or
                (((willQos.value) shl 3) and 0x18) or
                (((if (willFlag) 1 else 0) shl 2) and 0x4) or
                (((if (cleanStart) 1 else 0) shl 1) and 0x2) or
                ((if (reserved) 1 else 0) and 0x1)
        return flags.toUInt()
    }

    public companion object {
        public fun connectFlags(byte: Int): ConnectFlags {
            val reserved = (byte and 1) == 1
            if (reserved)
                throw MQTTException(ReasonCode.MALFORMED_PACKET)
            val willFlag = ((byte shr 2) and 1) == 1
            val willQos = ((byte shr 4) and 1) or ((byte shl 3) and 1)
            val willRetain = ((byte shr 5) and 1) == 1
            if (willFlag) {
                if (willQos == 3)
                    throw MQTTException(ReasonCode.MALFORMED_PACKET)
            } else {
                if (willQos != 0)
                    throw MQTTException(ReasonCode.MALFORMED_PACKET)
                if (willRetain)
                    throw MQTTException(ReasonCode.MALFORMED_PACKET)
            }

            return ConnectFlags(
                ((byte shr 7) and 1) == 1,
                ((byte shr 6) and 1) == 1,
                willRetain,
                Qos.valueOf(willQos)!!,
                willFlag,
                ((byte shr 1) and 1) == 1,
                reserved
            )
        }
    }
}
