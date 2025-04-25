package io.github.davidepianca98

import kotlinx.cinterop.*
import platform.posix.*
import platform.posix.SOL_SOCKET
import platform.posix.SO_RCVTIMEO
import platform.windows.*
import platform.windows.WSAEWOULDBLOCK
import platform.windows.WSAGetLastError
import io.github.davidepianca98.socket.IOException

public actual fun send(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.send(socket.convert(), buf, len, flags)
}

public actual fun recv(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.recv(socket.convert(), buf, len, flags)
}

public actual fun shutdown(socket: Int): Int {
    return platform.posix.shutdown(socket.convert(), platform.posix.SD_SEND)
}

public actual fun close(socket: Int): Int {
    return platform.posix.closesocket(socket.convert())
}

public actual fun memset(__s: CValuesRef<*>?, __c: Int, __n: ULong): CPointer<out CPointed>? {
    return platform.posix.memset(__s, __c, __n)
}

public actual fun sendto(
    __fd: Int,
    __buf: CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: UInt
): Long {
    return platform.posix.sendto(
        __fd.convert(),
        __buf as CValuesRef<ByteVar>?, __n.convert(), __flags, __addr, __addr_len.convert()
    ).convert()
}

public actual fun recvfrom(
    __fd: Int,
    __buf: CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: CValuesRef<UIntVarOf<UInt>>?
): Long {
    return platform.posix.recvfrom(
        __fd.convert(),
        __buf as CValuesRef<ByteVar>?, __n.convert(), __flags, __addr, __addr_len as CValuesRef<IntVarOf<Int>>?
    ).convert()
}

public actual fun inet_ntop(
    __af: Int,
    __cp: CValuesRef<*>?,
    __buf: CValuesRef<ByteVarOf<Byte>>?,
    __len: UInt
): CPointer<ByteVarOf<Byte>>? {
    return platform.windows.inet_ntop(__af, __cp as LPCVOID?, __buf as LPSTR?, __len.convert())
}

public actual fun inet_pton(__af: Int, __cp: String?, __buf: CValuesRef<*>?): Int {
    return platform.windows.inet_pton(__af, __cp, __buf as PVOID?)
}

public actual fun MemScope.sockaddrIn(sin_family: UShort, sin_port: UShort): sockaddr_in {
    val sockaddr = alloc<sockaddr_in>()
    platform.posix.memset(sockaddr.ptr, 0, sizeOf<sockaddr_in>().convert())
    sockaddr.sin_family = sin_family.convert()
    sockaddr.sin_port = posix_htons(sin_port.convert()).convert()
    return sockaddr
}

public actual fun MemScope.getPortFromSockaddrIn(sockaddr: sockaddr_in): UShort {
    return sockaddr.sin_port
}

public actual fun setsockopt(__fd: Int, __level: Int, __optname: Int, __optval: CValuesRef<*>?, __optlen: UInt): Int {
    return platform.posix.setsockopt(__fd.convert(), __level, __optname, __optval.toString(), __optlen.convert())
}

public actual fun bind(__fd: Int, __addr: CValuesRef<sockaddr>?, __len: UInt): Int {
    return platform.posix.bind(__fd.convert(), __addr, __len.toInt())
}

public actual fun set_non_blocking(__fd: Int): Int {
    memScoped {
        val on = alloc<uint32_tVar>()
        on.value = 1u
        return platform.posix.ioctlsocket(__fd.toULong(), platform.posix.FIONBIO.toInt(), on.ptr.reinterpret())
    }
}

public actual fun socket(__domain: Int, __type: Int, __protocol: Int): Int {
    return platform.posix.socket(__domain, __type, __protocol).toInt()
}

public actual fun connect(__fd: Int, __addr: CValuesRef<sockaddr>?, __len: UInt): Int {
    return platform.posix.connect(__fd.convert(), __addr, __len.toInt())
}

public actual fun accept(
    __fd: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: CValuesRef<UIntVarOf<UInt>>?
): Int {
    return platform.posix.accept(__fd.convert(), __addr, __addr_len as CValuesRef<IntVarOf<Int>>?).toInt()
}

public actual fun listen(__fd: Int, __n: Int): Int {
    return platform.posix.listen(__fd.convert(), __n)
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
    timeoutStruct.tv_usec = (timeout * 1000).toInt()
    return platform.windows.select(__nfds, __readfds, __writefds, __exceptfds, timeoutStruct.ptr)
}

public actual fun socketsInit() {
    memScoped {
        val wsaData = alloc<WSADATA>()
        if (platform.posix.WSAStartup(0x0202u, wsaData.ptr) != 0)
            throw IOException("Failed WSAStartup")
    }
}

public actual fun socketsCleanup() {
    platform.posix.WSACleanup()
}

public actual fun getErrno(): Int = WSAGetLastError()

public actual fun getEagain(): Int = WSAEWOULDBLOCK

public actual fun getEwouldblock(): Int = WSAEWOULDBLOCK

public actual fun MemScope.set_send_socket_timeout(__fd: Int, timeout: Long): Int {
    val timeoutValue = alloc<uint32_tVar>()
    timeoutValue.value = timeout.toUInt()
    return setsockopt(__fd, SOL_SOCKET, platform.posix.SO_SNDTIMEO, timeoutValue.ptr, sizeOf<uint32_tVar>().toUInt())
}

public actual fun MemScope.set_recv_socket_timeout(__fd: Int, timeout: Long): Int {
    val timeoutValue = alloc<uint32_tVar>()
    timeoutValue.value = timeout.toUInt()
    return setsockopt(__fd, SOL_SOCKET, SO_RCVTIMEO, timeoutValue.ptr, sizeOf<uint32_tVar>().toUInt())
}

public actual fun MemScope.getaddrinfo(name: String, service: String?): Pair<CPointer<sockaddr>, UInt>? {
    val hints = alloc<addrinfo>()
    platform.posix.memset(hints.ptr, 0, sizeOf<addrinfo>().convert())
    hints.ai_family = platform.posix.AF_INET
    hints.ai_socktype = platform.posix.SOCK_STREAM
    hints.ai_protocol = platform.posix.IPPROTO_TCP
    val result = alloc<CPointerVar<addrinfo>>()
    if (getaddrinfo(name, service, hints.ptr, result.ptr) == 0) {
        return if (result.pointed != null && result.pointed?.ai_addr != null && result.pointed?.ai_addrlen != null) {
            Pair(result.pointed?.ai_addr!!, result.pointed?.ai_addrlen!!.toUInt())
        } else {
            null
        }
    }
    return null
}

public actual fun fdSet(fd: Int, fdSet: CValuesRef<fd_set>) {
    posix_FD_SET(fd, fdSet)
}

public actual fun fdZero(fdSet: CValuesRef<fd_set>) {
    posix_FD_ZERO(fdSet)
}

public actual fun fdIsSet(fd: Int, fdSet: CValuesRef<fd_set>): Int {
    return posix_FD_ISSET(fd, fdSet)
}

public actual fun gettimeofday(timeval: timeval) {
    memScoped {
        val systemTime = alloc<SYSTEMTIME>()
        val fileTime = alloc<_FILETIME>()

        val epoch = 116444736000000000UL

        GetSystemTime(systemTime.ptr)
        SystemTimeToFileTime(systemTime.ptr, fileTime.ptr)
        val time = fileTime.dwLowDateTime.toULong() + (fileTime.dwHighDateTime.toULong() shl 32)

        timeval.tv_sec  = ((time - epoch) / 10000000UL).toInt()
        timeval.tv_usec = ((time - epoch).rem(10000000UL) / 10u).toInt()
    }
}
