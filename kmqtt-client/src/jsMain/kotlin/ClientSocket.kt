import socket.tcp.Socket

actual class ClientSocket actual constructor(address: String, port: Int, maximumPacketSize: Int, readTimeOut: Int) :
    Socket(net.createConnection("$address:$port"), { _, _ -> true }) // TODO use timeout
