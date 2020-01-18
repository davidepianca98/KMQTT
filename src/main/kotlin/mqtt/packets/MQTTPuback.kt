package mqtt.packets

class MQTTPuback : MQTTPacket {

    val validProperties = listOf(
        Property.REASON_STRING,
        Property.USER_PROPERTY
    )
}
