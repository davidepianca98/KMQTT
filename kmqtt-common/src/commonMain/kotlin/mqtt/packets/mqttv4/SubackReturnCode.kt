package mqtt.packets.mqttv4

import mqtt.packets.mqttv5.ReasonCode

public enum class SubackReturnCode(public val value: Int) {
    MAXIMUM_QOS0(0),
    MAXIMUM_QOS1(1),
    MAXIMUM_QOS2(2),
    FAILURE(128);

    public companion object {
        public fun valueOf(value: Int): SubackReturnCode? = values().firstOrNull { it.value == value }
    }
}

public fun List<ReasonCode>.toSubackReturnCodes(): List<SubackReturnCode> {
    return map {
        when (it) {
            ReasonCode.SUCCESS -> SubackReturnCode.MAXIMUM_QOS0
            ReasonCode.GRANTED_QOS1 -> SubackReturnCode.MAXIMUM_QOS1
            ReasonCode.GRANTED_QOS2 -> SubackReturnCode.MAXIMUM_QOS2
            else -> SubackReturnCode.FAILURE
        }
    }
}
