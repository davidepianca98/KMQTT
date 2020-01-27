package mqtt.packets

enum class Qos(val value: Int) {
    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2);

    companion object {
        fun valueOf(value: Int) = values().firstOrNull { it.value == value }
    }
}
