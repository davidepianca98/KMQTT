package socket

expect class ServerSocket(host: String, port: Int) {

    fun accept(): Socket

    fun bind(backlog: Int)
}
