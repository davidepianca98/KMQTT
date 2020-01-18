package mqtt.packets

class MQTTUnsuback : MQTTPacket {

    val validProperties = listOf(
        Property.REASON_STRING,
        Property.USER_PROPERTY
    )
}
