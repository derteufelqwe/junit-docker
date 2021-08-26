package de.derteufelqwe.junitInDocker

import de.derteufelqwe.junitInDocker.util.LogPacketHeader
import de.derteufelqwe.junitInDocker.util.LogSource
import kotlinx.coroutines.*
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.net.ServerSocket
import java.net.SocketException
import java.rmi.RemoteException
import java.util.*

class JUnitServiceImpl : JUnitService {

    private val socketServer = ServerSocket(9876)

    private val classCache = mutableMapOf<Int, RemoteClassLoader>()
    private val instanceCache = mutableMapOf<UUID, Any>()

    val consoleOut = System.out!!
    val consoleErr = System.err!!
    private val logOutBuffer = ByteArrayOutputStream()
    private val logErrBuffer = ByteArrayOutputStream()


    @Throws(RemoteException::class)
    override fun executeTest(classHash: Int, instanceId: UUID, className: String, method: String) {
        val rcl =
            classCache[classHash] ?: throw RuntimeException("Failed to load RemoteClassLoader for hash '$classHash'!")

        var clazz: Class<*> = rcl.loadClass(className)

        try {
            val inst = instanceCache.getOrPut(instanceId) { clazz.getConstructor().newInstance() }
            clazz.getMethod(method).invoke(inst)

        } catch (e: InvocationTargetException) {
            throw e.cause as Throwable
        }
    }

    override fun isClassCached(hash: Int): Boolean {
        return hash in classCache.keys
    }

    override fun loadRequiredClasses(mainClass: ClassInfo, dependClasses: List<ClassInfo>, hash: Int) {
        val rcl = RemoteClassLoader()

        for (cInfo in dependClasses) {
            rcl.bytes = cInfo.data
            rcl.loadClass(cInfo.name)
        }

        rcl.bytes = mainClass.data
        rcl.loadClass(mainClass.name)

        classCache[hash] = rcl
    }


    fun startLogServer() {
        consoleOut.println("Starting log server")
        runBlocking {

            while (isActive) {
                val socket = socketServer.accept()
                consoleOut.println("Accepted socket ${socket.port}")

                launch(Dispatchers.IO) {
                    val output = socket.getOutputStream()

                    try {
                        while (isActive) {
                            if (logOutBuffer.size() == 0 && logErrBuffer.size() == 0) {
                                continue
                            }

                            // Check if the current connection is actually still alive by trying to send a keepalive packet
                            try {
                                writeHeader(LogPacketHeader(keepAlive = true), output)
                            } catch (e: SocketException) {
                                break
                            }

                            // Send the logs
                            if (logOutBuffer.size() != 0) {
                                sendBuffer(logOutBuffer, output, LogSource.OUT)
                            }
                            if (logErrBuffer.size() != 0) {
                                sendBuffer(logErrBuffer, output, LogSource.ERR)
                            }
                        }

                    } finally {
                        output.close()
                        socket.close()
                        consoleOut.println("Closed socket ${socket.port}")
                    }
                }

            }
        }
    }

    fun redirectConsole() {
        System.setOut(PrintStream(logOutBuffer))
        System.setErr(PrintStream(logErrBuffer))
    }

    fun restoreConsole() {
        System.setOut(consoleOut)
        System.setErr(consoleErr)
    }

    private fun writeHeader(header: LogPacketHeader, out: OutputStream) {
        val oOut = ObjectOutputStream(out)
        oOut.writeObject(header)
        oOut.flush()
    }

    private fun sendBuffer(buffer: ByteArrayOutputStream, out: OutputStream, source: LogSource) {
        val data = buffer.toByteArray()
        buffer.reset()

        try {
            writeHeader(LogPacketHeader(source, data.size), out)
            out.write(data)
            out.flush()

        } catch (e: IOException) {
            consoleOut.println("Sending logs failed")
        }
    }

}
