package io.github.davidepianca98

import kotlinx.cinterop.*
import platform.posix.*

public actual fun send(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.send(socket.convert(), buf, len.convert(), flags or MSG_NOSIGNAL).convert()
}

public actual fun recv(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.recv(socket, buf, len.convert(), flags).convert()
}

public actual fun shutdown(socket: Int): Int {
    return shutdown(socket, SHUT_WR)
}

public actual fun close(socket: Int): Int {
    return platform.posix.close(socket)
}

public actual fun memset(__s: CValuesRef<*>?, __c: Int, __n: ULong): CPointer<out CPointed>? {
    return platform.posix.memset(__s, __c, __n.convert())
}

public actual fun sendto(
    __fd: Int,
    __buf: CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: UInt
): Long {
    return platform.posix.sendto(__fd, __buf, __n.convert(), __flags, __addr, __addr_len).convert()
}

public actual fun recvfrom(
    __fd: Int,
    __buf: CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: CValuesRef<UIntVarOf<UInt>>?
): Long {
    return platform.posix.recvfrom(__fd, __buf, __n.convert(), __flags, __addr, __addr_len).convert()
}

public actual fun inet_ntop(
    __af: Int,
    __cp: CValuesRef<*>?,
    __buf: CValuesRef<ByteVarOf<Byte>>?,
    __len: UInt
): CPointer<ByteVarOf<Byte>>? {
    return platform.darwin.inet_ntop(__af, __cp, __buf, __len)
}

public actual fun inet_pton(__af: Int, __cp: String?, __buf: CValuesRef<*>?): Int {
    return platform.darwin.inet_pton(__af, __cp, __buf)
}

private fun htons(value: UShort): UShort {
    return if (BYTE_ORDER == BIG_ENDIAN) {
        value
    } else {
        platform.builtin.builtin_bswap16(value.convert()).convert()
    }
}

public actual fun MemScope.sockaddrIn(sin_family: UShort, sin_port: UShort): sockaddr_in {
    val sockaddr = alloc<sockaddr_in>()
    platform.posix.memset(sockaddr.ptr, 0, sizeOf<sockaddr_in>().convert())
    sockaddr.sin_family = sin_family.toUByte()
    sockaddr.sin_port = htons(sin_port)
    return sockaddr
}

public actual fun MemScope.getPortFromSockaddrIn(sockaddr: sockaddr_in): UShort {
    return sockaddr.sin_port
}

public actual fun setsockopt(__fd: Int, __level: Int, __optname: Int, __optval: CValuesRef<*>?, __optlen: UInt): Int {
    return platform.posix.setsockopt(__fd.convert(), __level, __optname, __optval, __optlen.convert())
}

public actual fun bind(__fd: Int, __addr: CValuesRef<sockaddr>?, __len: UInt): Int {
    return platform.posix.bind(__fd, __addr, __len)
}

public actual fun set_non_blocking(__fd: Int): Int {
    return fcntl(__fd, F_SETFL, O_NONBLOCK)
}

public actual fun socket(__domain: Int, __type: Int, __protocol: Int): Int {
    return platform.posix.socket(__domain, __type, __protocol)
}

public actual fun connect(__fd: Int, __addr: CValuesRef<sockaddr>?, __len: UInt): Int {
    return platform.posix.connect(__fd, __addr, __len)
}

public actual fun accept(__fd: Int, __addr: CValuesRef<sockaddr>?, __addr_len: CValuesRef<UIntVarOf<UInt>>?): Int {
    return platform.posix.accept(__fd, __addr, __addr_len)
}

public actual fun listen(__fd: Int, __n: Int): Int {
    return platform.posix.listen(__fd, __n)
}

public actual fun MemScope.select(
    __nfds: Int,
    __readfds: CValuesRef<fd_set>?,
    __writefds: CValuesRef<fd_set>?,
    __exceptfds: CValuesRef<fd_set>?,
    timeout: Long
): Int {
    val timeoutStruct = alloc<timeval>()
    timeoutStruct.tv_sec = 0
    timeoutStruct.tv_usec = timeout.toInt() * 1000
    return select(__nfds, __readfds, __writefds, __exceptfds, timeoutStruct.ptr)
}

public actual fun socketsInit() {}

public actual fun socketsCleanup() {}

public actual fun getErrno(): Int = errno

public actual fun getEagain(): Int = EAGAIN

public actual fun getEwouldblock(): Int = EWOULDBLOCK

public actual fun MemScope.set_send_socket_timeout(__fd: Int, timeout: Long): Int {
    val timeoutStruct = alloc<timeval>()
    val seconds = timeout / 1000
    timeoutStruct.tv_sec = seconds.convert()
    timeoutStruct.tv_usec = ((timeout - seconds * 1000) * 1000).convert()
    return setsockopt(__fd, SOL_SOCKET, SO_SNDTIMEO, timeoutStruct.ptr, sizeOf<timeval>().toUInt())
}

public actual fun MemScope.set_recv_socket_timeout(__fd: Int, timeout: Long): Int {
    val timeoutStruct = alloc<timeval>()
    val seconds = timeout / 1000
    timeoutStruct.tv_sec = seconds.convert()
    timeoutStruct.tv_usec = ((timeout - seconds * 1000) * 1000).convert()
    return setsockopt(__fd, SOL_SOCKET, SO_RCVTIMEO, timeoutStruct.ptr, sizeOf<timeval>().toUInt())
}

public actual fun MemScope.getaddrinfo(name: String, service: String?): Pair<CPointer<sockaddr>, UInt>? {
    val hints = alloc<addrinfo>()
    platform.posix.memset(hints.ptr, 0, sizeOf<addrinfo>().convert())
    hints.ai_family = platform.posix.AF_UNSPEC
    hints.ai_socktype = platform.posix.SOCK_STREAM
    hints.ai_protocol = platform.posix.IPPROTO_TCP
    val result = alloc<CPointerVar<addrinfo>>()
    if (getaddrinfo(name, service, hints.ptr, result.ptr) == 0) {
        return if (result.pointed != null && result.pointed?.ai_addr != null && result.pointed?.ai_addrlen != null) {
            Pair(result.pointed?.ai_addr!!, result.pointed?.ai_addrlen!!)
        } else {
            null
        }
    }
    return null
}

public actual fun fdSet(fd: Int, fdSet: CValuesRef<fd_set>) {
    __darwin_fd_set(fd, fdSet)
}

public actual fun fdZero(fdSet: CValuesRef<fd_set>) {
    memset(fdSet, 0, sizeOf<fd_set>().convert())
}

public actual fun fdIsSet(fd: Int, fdSet: CValuesRef<fd_set>): Int {
    return __darwin_fd_isset(fd, fdSet)
}

public actual fun gettimeofday(timeval: timeval) {
    gettimeofday(timeval.ptr, null)
}
