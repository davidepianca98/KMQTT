package mqtt.packets

class MQTTConnect(flags: Int, data: ByteArray) : MQTTPacket {

    init {
        checkFlags(flags)
    }
}
