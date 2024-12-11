package io.github.davidepianca98.mqtt

import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode

public class MQTTException(public val reasonCode: ReasonCode) : Exception(reasonCode.toString())
