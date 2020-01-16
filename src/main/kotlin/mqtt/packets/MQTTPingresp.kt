package mqtt.packets

class MQTTPingresp(flags: Int, data: ByteArray) : MQTTPacket {

    init {
        checkFlags(flags)
    }
}
