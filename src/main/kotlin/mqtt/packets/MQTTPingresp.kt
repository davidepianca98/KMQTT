package mqtt.packets

class MQTTPingresp : MQTTPacket {
    override fun toByteArray(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object : MQTTDeserializer {
        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPacket {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}
