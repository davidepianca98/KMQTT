package mqtt.packets

class MQTTAuth : MQTTPacket {

    private val validProperties = listOf(
        Property.AUTHENTICATION_METHOD,
        Property.AUTHENTICATION_DATA,
        Property.REASON_STRING,
        Property.USER_PROPERTY
    )
}
