package mqtt.packets.mqttv3_1_1

enum class SubackReturnCode(val value: Int) {
    MAXIMUM_QOS0(0),
    MAXIMUM_QOS1(1),
    MAXIMUM_QOS2(2),
    FAILURE(128);

    companion object {
        fun valueOf(value: Int) = values().firstOrNull { it.value == value }
    }
}
