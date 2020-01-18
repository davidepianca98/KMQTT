package mqtt.packets

class MQTTSuback : MQTTPacket {

    val validProperties = listOf(
        Property.REASON_STRING,
        Property.USER_PROPERTY
    )
}
