package mqtt

import mqtt.packets.mqttv5.ReasonCode

public class MQTTException(public val reasonCode: ReasonCode) : Exception(reasonCode.toString())
