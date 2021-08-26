package de.derteufelqwe.junit4Docker.util

import java.io.IOException
import java.io.InputStream
import java.util.*

@Throws(IOException::class)
fun InputStream.readNBytes(len: Int): ByteArray {
    require(len >= 0) { "len < 0" }

    var bufs: MutableList<ByteArray>? = null
    var result: ByteArray? = null
    var total = 0
    var remaining = len
    var n: Int
    do {
        val buf = ByteArray(Math.min(remaining, 8192))
        var nread = 0

        // read to EOF which may read more or less than buffer size
        while (read(
                buf, nread,
                Math.min(buf.size - nread, remaining)
            ).also { n = it } > 0
        ) {
            nread += n
            remaining -= n
        }
        if (nread > 0) {
            if (Int.MAX_VALUE - 8 - total < nread) {
                throw OutOfMemoryError("Required array size too large")
            }
            total += nread
            if (result == null) {
                result = buf
            } else {
                if (bufs == null) {
                    bufs = ArrayList()
                    bufs.add(result)
                }
                bufs.add(buf)
            }
        }
        // if the last call to read returned -1 or the number of bytes
        // requested have been read then break
    } while (n >= 0 && remaining > 0)

    if (bufs == null) {
        if (result == null) {
            return ByteArray(0)
        }
        return if (result.size == total) result else Arrays.copyOf(result, total)
    }

    result = ByteArray(total)
    var offset = 0
    remaining = total
    for (b in bufs) {
        val count = Math.min(b.size, remaining)
        System.arraycopy(b, 0, result, offset, count)
        offset += count
        remaining -= count
    }

    return result
}