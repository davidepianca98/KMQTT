package mqtt.packets

class MQTTDisconnect(flags: Int, data: ByteArray) : MQTTPacket {

    init {
        checkFlags(flags)
    }
}
