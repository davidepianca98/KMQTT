package mqtt.packets

import mqtt.packets.mqttv5.MQTTSerializer

public interface MQTTPacket : MQTTSerializer {

    public fun resizeIfTooBig(maximumPacketSize: UInt): Boolean = true
}
