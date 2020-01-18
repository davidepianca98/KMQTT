package mqtt.packets

class MQTTUnsubscribe : MQTTPacket {

    val validProperties = listOf(
        Property.USER_PROPERTY
    )

    override fun checkFlags(flags: Int) {
        require(flags.flagsBit(0) == 0)
        require(flags.flagsBit(1) == 1)
        require(flags.flagsBit(2) == 0)
        require(flags.flagsBit(3) == 0)
    }
}
