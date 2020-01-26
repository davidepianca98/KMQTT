package socket

expect class ServerSocket(host: String, port: Int, backlog: Int) {

    suspend fun accept(): Socket

    fun close()
}
