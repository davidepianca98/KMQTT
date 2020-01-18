package mqtt

import mqtt.packets.ReasonCode

class MalformedPacketException(reasonCode: ReasonCode) : Exception()
