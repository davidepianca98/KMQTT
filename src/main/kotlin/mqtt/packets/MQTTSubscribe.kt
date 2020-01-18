package mqtt.packets

class MQTTSubscribe : MQTTPacket {

    val validProperties = listOf(
        Property.SUBSCRIPTION_IDENTIFIER,
        Property.USER_PROPERTY
    )

    override fun checkFlags(flags: Int) {
        require(flags.flagsBit(0) == 0)
        require(flags.flagsBit(1) == 1)
        require(flags.flagsBit(2) == 0)
        require(flags.flagsBit(3) == 0)
    }
}
