package mqtt.packets

class MQTTAuth : MQTTPacket {

    override fun toByteArray(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.AUTHENTICATION_METHOD,
            Property.AUTHENTICATION_DATA,
            Property.REASON_STRING,
            Property.USER_PROPERTY
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPacket {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}
