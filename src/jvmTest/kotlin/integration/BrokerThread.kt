package integration

import mqtt.Broker

class BrokerThread(private val broker: Broker = Broker()) : Thread() {

    override fun run() {
        broker.listen()
    }

    fun stopBroker() {
        broker.stop()
    }

}
