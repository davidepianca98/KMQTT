package mqtt.packets

class MQTTPubcomp : MQTTPacket {

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
    }
}
