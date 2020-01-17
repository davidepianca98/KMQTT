package mqtt

import mqtt.packets.ReasonCodes

class MalformedPacketException(reasonCode: ReasonCodes) : Exception()
