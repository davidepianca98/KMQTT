package mqtt

enum class MQTTControlPacketType {
    Reserved,
    CONNECT,
    CONNACK,
    PUBLISH,
    PUBACK,
    PUBREC,
    PUBREL,
    PUBCOMP,
    SUBSCRIBE,
    SUBACK,
    UNSUBSCRIBE,
    UNSUBACK,
    PINGREQ,
    PINGRESP,
    DISCONNECT,
    AUTH;

    companion object {
        fun valueOf(value: Int) = values().firstOrNull { it.ordinal == value }
    }
}
