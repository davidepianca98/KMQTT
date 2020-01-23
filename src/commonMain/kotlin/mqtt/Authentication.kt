package mqtt

interface Authentication {

    fun authenticate(username: String?, password: UByteArray?): Boolean
}
