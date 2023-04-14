package socket

public interface SocketInterface {

    public fun send(data: UByteArray)

    public fun sendRemaining()

    public fun read(): UByteArray?

    public fun close()
}
