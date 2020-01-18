package mqtt.packets

class MQTTDisconnect(val reasonCode: ReasonCode) : MQTTPacket, MQTTSerializer {

    override fun toByteArray(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.SESSION_EXPIRY_INTERVAL,
            Property.SERVER_REFERENCE,
            Property.REASON_STRING,
            Property.USER_PROPERTY
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPacket {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}
