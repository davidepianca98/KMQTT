package mqtt.packets

class MQTTSubscribe(flags: Int, data: ByteArray) : MQTTPacket {

    init {
        checkFlags(flags)
    }

    override fun checkFlags(flags: Int) {
        require(flags.flagsBit(0) == 0)
        require(flags.flagsBit(1) == 1)
        require(flags.flagsBit(2) == 0)
        require(flags.flagsBit(3) == 0)
    }
}
