package mqtt.packets

class MQTTPubrec : MQTTPacket {

    val validProperties = listOf(
        Property.REASON_STRING,
        Property.USER_PROPERTY
    )
}
