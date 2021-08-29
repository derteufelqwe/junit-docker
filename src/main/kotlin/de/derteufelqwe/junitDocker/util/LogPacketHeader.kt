package de.derteufelqwe.junitDocker.util

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class LogPacketHeader(val source: LogSource = LogSource.OUT, val length: Int = 0, val keepAlive: Boolean = false) : Serializable {

    companion object {
        // Calculates the size of the header packet
        val size: Int by lazy {
            val out = ByteArrayOutputStream()
            val oOut = ObjectOutputStream(out)
            oOut.writeObject(LogPacketHeader())

            out.size()
        }
    }

}


enum class LogSource {
    OUT,
    ERR,
}
