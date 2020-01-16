package mqtt.packets

class MQTTPublish(flags: Int, data: ByteArray) : MQTTPacket {

    init {
        checkFlags(flags)
    }

    override fun checkFlags(flags: Int) {

    }
}
