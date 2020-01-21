package mqtt

interface Authentication {

    fun authenticate(username: String?, password: ByteArray?): Boolean
}
