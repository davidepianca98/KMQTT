package socket

import java.nio.channels.SelectionKey

interface ServerSocketImpl {

    fun accept(key: SelectionKey)
}
