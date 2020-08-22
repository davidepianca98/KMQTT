package mqtt.packets

import mqtt.packets.mqttv5.MQTTSerializer

interface MQTTPacket : MQTTSerializer {

    fun resizeIfTooBig(maximumPacketSize: UInt): Boolean = true
}
