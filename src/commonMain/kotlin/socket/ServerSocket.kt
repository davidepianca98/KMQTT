package socket

expect class ServerSocket(host: String, port: Int, backlog: Int) {

    fun accept(): Socket

    fun close()
}
