package mqtt.packets

interface MQTTPacket {

    fun Int.flagsBit(bit: Int): Int {
        require(bit in 0..3)
        return (this shr bit) and 0b1
    }

    fun checkFlags(flags: Int) {
        require(flags.flagsBit(0) == 0)
        require(flags.flagsBit(1) == 0)
        require(flags.flagsBit(2) == 0)
        require(flags.flagsBit(3) == 0)
    }
}
