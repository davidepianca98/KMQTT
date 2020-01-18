package mqtt

import mqtt.packets.ReasonCode

class MalformedPacketException(val reasonCode: ReasonCode) : Exception()
