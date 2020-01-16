package mqtt.packets

class MQTTUnsuback(flags: Int, data: ByteArray) : MQTTPacket {

    init {
        checkFlags(flags)
    }
}
