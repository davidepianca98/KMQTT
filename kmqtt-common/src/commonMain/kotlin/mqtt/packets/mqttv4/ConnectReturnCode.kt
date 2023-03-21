package mqtt.packets.mqttv4

enum class ConnectReturnCode(val value: Int) {
    CONNECTION_ACCEPTED(0),
    UNACCEPTABLE_PROTOCOL_VERSION(1),
    IDENTIFIER_REJECTED(2),
    SERVER_UNAVAILABLE(3),
    BAD_USERNAME_PASSWORD(4),
    NOT_AUTHORIZED(5);

    companion object {
        fun valueOf(value: Int) = values().firstOrNull { it.value == value }
    }
}
