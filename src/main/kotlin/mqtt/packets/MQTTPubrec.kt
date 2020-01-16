package mqtt.packets

class MQTTPubrec(flags: Int, data: ByteArray) : MQTTPacket {

    init {
        checkFlags(flags)
    }
}
