package mqtt.packets

class MQTTDisconnect : MQTTPacket {

    private val validProperties = listOf(
        Property.SESSION_EXPIRY_INTERVAL,
        Property.SERVER_REFERENCE,
        Property.REASON_STRING,
        Property.USER_PROPERTY
    )
}
