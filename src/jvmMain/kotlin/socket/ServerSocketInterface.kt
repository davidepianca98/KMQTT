package socket

import java.nio.channels.SelectionKey

interface ServerSocketInterface {

    fun accept(key: SelectionKey)
}
