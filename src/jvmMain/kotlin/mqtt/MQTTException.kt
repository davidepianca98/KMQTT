package mqtt

import mqtt.packets.ReasonCode

class MQTTException(val reasonCode: ReasonCode) : Exception()
