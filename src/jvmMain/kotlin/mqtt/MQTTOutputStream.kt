package mqtt

import mqtt.packets.MQTTSerializer
import java.io.DataOutputStream
import java.io.OutputStream

class MQTTOutputStream(outputStream: OutputStream) : DataOutputStream(outputStream) {

    fun writePacket(packet: MQTTSerializer) {
        write(packet.toByteArray().toByteArray())
        flush()
    }
}
