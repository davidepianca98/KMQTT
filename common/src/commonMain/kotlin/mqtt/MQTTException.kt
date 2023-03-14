package mqtt

import mqtt.packets.mqttv5.ReasonCode

class MQTTException(val reasonCode: ReasonCode) : Exception()
