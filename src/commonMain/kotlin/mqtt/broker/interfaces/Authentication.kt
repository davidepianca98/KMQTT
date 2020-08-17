package mqtt.broker.interfaces

interface Authentication {

    fun authenticate(username: String?, password: UByteArray?): Boolean
}
