package io.github.davidepianca98

import kotlinx.cinterop.*
import platform.posix.fd_set
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.timeval

public expect fun send(
    socket: Int,
    buf: CValuesRef<ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>?,
    len: Int,
    flags: Int
): Int

public expect fun recv(
    socket: Int,
    buf: CValuesRef<ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>?,
    len: Int,
    flags: Int
): Int

public expect fun shutdown(socket: Int): Int

public expect fun close(socket: Int): Int

public expect fun memset(
    __s: CValuesRef<*>?,
    __c: Int,
    __n: ULong
): CPointer<out CPointed>?

public expect fun sendto(
    __fd: Int,
    __buf: CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: UInt
): Long

public expect fun recvfrom(
    __fd: Int,
    __buf: CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: CValuesRef<UIntVarOf<UInt>>?
): Long

public expect fun inet_ntop(
    __af: Int,
    __cp: CValuesRef<*>?,
    __buf: CValuesRef<ByteVarOf<Byte>>?,
    __len: UInt
): CPointer<ByteVarOf<Byte>>?

public expect fun inet_pton(__af: Int, __cp: String?, __buf: CValuesRef<*>?): Int

public expect fun MemScope.sockaddrIn(sin_family: UShort, sin_port: UShort): sockaddr_in

public expect fun MemScope.getPortFromSockaddrIn(sockaddr: sockaddr_in): UShort

public expect fun setsockopt(
    __fd: Int,
    __level: Int,
    __optname: Int,
    __optval: CValuesRef<*>?,
    __optlen: UInt
): Int

public expect fun bind(__fd: Int, __addr: CValuesRef<sockaddr>?, __len: UInt): Int

public expect fun set_non_blocking(__fd: Int): Int

public expect fun MemScope.set_send_socket_timeout(__fd: Int, timeout: Long): Int

public expect fun MemScope.set_recv_socket_timeout(__fd: Int, timeout: Long): Int

public expect fun socket(__domain: Int, __type: Int, __protocol: Int): Int

public expect fun connect(__fd: Int, __addr: CValuesRef<sockaddr>?, __len: UInt): Int

public expect fun accept(
    __fd: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: CValuesRef<UIntVarOf<UInt>>?
): Int

public expect fun listen(__fd: Int, __n: Int): Int

public expect fun MemScope.select(
    __nfds: Int,
    __readfds: CValuesRef<fd_set>?,
    __writefds: CValuesRef<fd_set>?,
    __exceptfds: CValuesRef<fd_set>?,
    timeout: Long
): Int

public expect fun MemScope.getaddrinfo(name: String, service: String?): Pair<CPointer<sockaddr>, UInt>?

public expect fun fdSet(fd: Int, fdSet: CValuesRef<fd_set>)

public expect fun fdZero(fdSet: CValuesRef<fd_set>)

public expect fun fdIsSet(fd: Int, fdSet: CValuesRef<fd_set>): Int

public expect fun socketsInit()

public expect fun socketsCleanup()

public expect fun getErrno(): Int

public expect fun getEagain(): Int

public expect fun getEwouldblock(): Int

public typealias socklen_tVar = UIntVarOf<UInt>

public expect fun gettimeofday(timeval: timeval)
