package io.github.davidepianca98.mqtt.packets

import io.github.davidepianca98.mqtt.packets.mqttv5.MQTTSerializer

public interface MQTTPacket : MQTTSerializer {

    public fun resizeIfTooBig(maximumPacketSize: UInt): Boolean = true
}
