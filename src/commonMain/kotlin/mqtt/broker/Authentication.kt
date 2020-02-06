package mqtt.broker

interface Authentication {

    fun authenticate(username: String?, password: UByteArray?): Boolean
}
