package io.github.davidepianca98.mqtt.packets

public enum class Qos(public val value: Int) {
    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2);

    public companion object {
        public fun valueOf(value: Int): Qos? = entries.firstOrNull { it.value == value }

        public fun min(qos1: Qos, qos2: Qos): Qos = valueOf(kotlin.math.min(qos1.value, qos2.value))!!
    }
}
