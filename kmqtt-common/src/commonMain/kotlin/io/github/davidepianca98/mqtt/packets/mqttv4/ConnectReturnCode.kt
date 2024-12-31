package io.github.davidepianca98.mqtt.packets.mqttv4

import io.github.davidepianca98.mqtt.packets.mqttv4.ConnectReturnCode.*
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode

public enum class ConnectReturnCode(public val value: Int) {
    CONNECTION_ACCEPTED(0),
    UNACCEPTABLE_PROTOCOL_VERSION(1),
    IDENTIFIER_REJECTED(2),
    SERVER_UNAVAILABLE(3),
    BAD_USERNAME_PASSWORD(4),
    NOT_AUTHORIZED(5);

    public companion object {
        public fun valueOf(value: Int): ConnectReturnCode? = entries.firstOrNull { it.value == value }
    }
}

public fun ConnectReturnCode.toReasonCode() : ReasonCode = when (this) {
    CONNECTION_ACCEPTED -> ReasonCode.SUCCESS
    UNACCEPTABLE_PROTOCOL_VERSION -> ReasonCode.UNSUPPORTED_PROTOCOL_VERSION
    IDENTIFIER_REJECTED -> ReasonCode.CLIENT_IDENTIFIER_NOT_VALID
    SERVER_UNAVAILABLE -> ReasonCode.SERVER_UNAVAILABLE
    BAD_USERNAME_PASSWORD -> ReasonCode.BAD_USER_NAME_OR_PASSWORD
    NOT_AUTHORIZED -> ReasonCode.NOT_AUTHORIZED
}
