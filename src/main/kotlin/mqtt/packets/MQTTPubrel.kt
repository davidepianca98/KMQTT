package mqtt.packets

class MQTTPubrel : MQTTPacket {

    override fun toByteArray(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object : MQTTDeserializer {

        val validProperties = listOf(
            Property.REASON_STRING,
            Property.USER_PROPERTY
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPacket {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun checkFlags(flags: Int) {
            require(flags.flagsBit(0) == 0)
            require(flags.flagsBit(1) == 1)
            require(flags.flagsBit(2) == 0)
            require(flags.flagsBit(3) == 0)
        }
    }
}
