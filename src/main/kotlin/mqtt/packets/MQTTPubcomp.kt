package mqtt.packets

class MQTTPubcomp : MQTTPacket {

    val validProperties = listOf(
        Property.REASON_STRING,
        Property.USER_PROPERTY
    )
}
