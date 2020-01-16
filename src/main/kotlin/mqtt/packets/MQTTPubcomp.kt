package mqtt.packets

class MQTTPubcomp(flags: Int, data: ByteArray) : MQTTPacket {

    init {
        checkFlags(flags)
    }
}
