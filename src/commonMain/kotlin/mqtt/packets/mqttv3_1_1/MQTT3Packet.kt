package mqtt.packets.mqttv3_1_1

import mqtt.packets.MQTTPacket
import mqtt.packets.mqttv5.MQTTSerializer

interface MQTT3Packet : MQTTSerializer, MQTTPacket
