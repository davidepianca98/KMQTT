package mqtt.packets

public enum class MQTTControlPacketType(public val value: Int) {
    Reserved(0),
    CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    PUBREC(5),
    PUBREL(6),
    PUBCOMP(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSUBSCRIBE(10),
    UNSUBACK(11),
    PINGREQ(12),
    PINGRESP(13),
    DISCONNECT(14),
    AUTH(15);

    public companion object {
        public fun valueOf(value: Int): MQTTControlPacketType? = values().firstOrNull { it.value == value }
    }
}
