package socket

import socket.streams.InputStream
import socket.streams.OutputStream

actual class Socket {
    actual var soTimeout: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    actual fun close() {
    }

    actual fun getInputStream(): InputStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun getOutputStream(): OutputStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
