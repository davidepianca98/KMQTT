package io.github.davidepianca98.socket

/**
 * Default size of the socket I/O buffers in bytes.
 *
 * This only controls the size of the chunks read from and written to the socket:
 * packets larger than the buffer are reassembled incrementally, so this value is
 * independent of the MQTT maximum packet size.
 */
public const val DEFAULT_SOCKET_BUFFER_SIZE: Int = 16 * 1024
