package de.derteufelqwe.junit4Docker

import de.derteufelqwe.junit4Docker.util.*
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Before
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import java.io.*
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.rmi.registry.LocateRegistry
import java.util.*
import kotlin.reflect.full.companionObject


class DockerRunner(testClass: Class<*>) : BlockJUnit4ClassRunner(testClass) {

    // Matches every newline except if it's the last char in the string
    private val RE_MATCH_NEWLINE = Regex("(\\n)(?!\$)")

    private var globalContainerInfo: ContainerInfo? = null  // Used for the runAllInOne config
    private var globalLogServer: LogReceiverInfo? = null
    private var globalJUnitService: JUnitService? = null


    override fun run(notifier: RunNotifier) {
        runBlocking {
            // Find the ContainerProvider method
            val containerProviders = testClass.getAnnotatedMethods(ContainerProvider::class.java)
            val containerDestroyers = testClass.getAnnotatedMethods(ContainerDestroyer::class.java)

            if (containerProviders.size != 1) {
                throw RemoteJUnitException("You need exactly 1 ContainerProvider. Found ${containerProviders.size}")
            }
            if (containerDestroyers.size != 1) {
                throw RemoteJUnitException("You need exactly 1 ContainerDestroyer. Found ${containerDestroyers.size}")
            }

            val provMethod = containerProviders[0].method
            val destrMethod = containerDestroyers[0].method
            validateContainerProvider(provMethod)
            validateContainerDestroyer(destrMethod)


            // Read the config object
            val config = testClass.getAnnotation(RemoteJUnitConfig::class.java)


            // Create the global container info if required
            if (config?.reuseContainer == true) {
                globalContainerInfo = provMethod.invoke(null) as ContainerInfo
            }


            // Find @Before and @After annotations
            val beforeMethods = testClass.getAnnotatedMethods(Before::class.java).map { it.name }
            val afterMethods = testClass.getAnnotatedMethods(After::class.java).map { it.name }


            // Execute the tests
            val instanceId = UUID.randomUUID()
            for (child in description.children) {
                val containerInfo = getContainerInfo(config, provMethod)
                delay(config?.containerStartupDelay ?: 1000L)   // Wait for the container to get up and running
                val logReceiver = getLogReceiver(config, this, containerInfo.host, containerInfo.logPort)
                val jUnitService = getJUnitService(config, containerInfo.host, containerInfo.rmiPort)
                val hash = sendRequiredClasses(jUnitService, child.testClass)

                notifier.fireTestStarted(child)

                try {
                    // Run the required test methods include @Before and @After methods
                    beforeMethods.forEach {
                        jUnitService.executeTest(hash, instanceId, testClass.name, it)
                    }
                    jUnitService.executeTest(hash, instanceId, testClass.name, child.methodName)
                    afterMethods.forEach {
                        jUnitService.executeTest(hash, instanceId, testClass.name, it)
                    }

                    delay(config?.logReceiveDelay ?: 200L)
                    notifier.fireTestFinished(child)

                } catch (e: Exception) {
                    delay(config?.logReceiveDelay ?: 200L)
                    notifier.fireTestFailure(Failure(child, e))

                } finally {
                    stopLogReceiver(config, logReceiver)
                    destroyContainerInfo(config, containerInfo, destrMethod)
                }

            }

            // Call destroy for the global objects
            globalLogServer?.let {
                stopLogReceiver(config, it, true)
            }

            globalContainerInfo?.let {
                destroyContainerInfo(config, it, destrMethod, true)
            }
        }

    }


    @Throws(RemoteJUnitException::class)
    private fun validateContainerProvider(method: Method) {
        if (!Modifier.isStatic(method.modifiers)) {
            throw RemoteJUnitException("@ContainerProvider must be static.")
        }

        if (method.returnType != ContainerInfo::class.java) {
            throw RemoteJUnitException("@ContainerProvider must return ${ContainerInfo::class.qualifiedName}.")
        }

        if (method.parameters.isNotEmpty()) {
            throw RemoteJUnitException("@ContainerProvider must not take any arguments.")
        }
    }

    @Throws(RemoteJUnitException::class)
    private fun validateContainerDestroyer(method: Method) {
        if (!Modifier.isStatic(method.modifiers)) {
            throw RemoteJUnitException("@ContainerDestroyer must be static.")
        }

        if (method.returnType.typeName != "void") {
            throw RemoteJUnitException("@ContainerDestroyer must return nothing.")
        }

        val params = method.parameters
        if (params.size != 1 || params[0].type != ContainerInfo::class.java) {
            throw RemoteJUnitException("@ContainerDestroyer must have exactly one parameter of type ${ContainerInfo::class.qualifiedName}.")
        }
    }

    /**
     * Returns the hash of the main test class
     */
    private fun sendRequiredClasses(jUnitService: JUnitService, testClass: Class<*>): Int {
        val requiredClasses = testClass.getAnnotation(RequiredClasses::class.java)?.data ?: arrayOf()

        val mainClassInfo = constructClassInfo(testClass)
        val hash = mainClassInfo.data.contentHashCode()

        // Only load the classes if they aren't cached on the server
        if (jUnitService.isClassCached(hash)) {
            return hash
        }

        val classInfos = requiredClasses.map {
            constructClassInfo(it.java)
        }.toMutableList()

        // Add potential Companion object
        val compantionInfos = requiredClasses.map {
            val comp = it.companionObject
            if (comp != null) {
                constructClassInfo(comp.java)
            } else {
                null
            }
        }.filterNotNull()

        classInfos.addAll(compantionInfos)

        val companion = testClass.kotlin.companionObject
        if (companion != null) {
            classInfos.add(constructClassInfo(companion.java))
        }

        jUnitService.loadRequiredClasses(mainClassInfo, classInfos, hash)

        return hash
    }

    private fun constructClassInfo(clazz: Class<*>): ClassInfo {
        val classFile = clazz.classLoader.getResource(clazz.name.replace(".", "/") + ".class")?.path
            ?: throw RuntimeException("Failed to find class $clazz")
        val bytes = File(classFile).readBytes()

        return ClassInfo(clazz.name, bytes)
    }

    private fun getContainerInfo(config: RemoteJUnitConfig?, provMethod: Method): ContainerInfo {
        if (config?.reuseContainer == true) {
            if (globalContainerInfo == null) {
                globalContainerInfo = provMethod.invoke(null) as ContainerInfo
            }

            return globalContainerInfo as ContainerInfo
        }

        return provMethod.invoke(null) as ContainerInfo
    }

    private fun startLogReceiver(scope: CoroutineScope, host: String, port: Int): LogReceiverInfo {
        val socket = Socket(host, port)

        val input = socket.getInputStream()
        val reader = input.bufferedReader(StandardCharsets.UTF_8)

        val job = scope.launch(Dispatchers.IO) {

            while (isActive) {
                try {
                    val headerData = input.readNBytes(LogPacketHeader.size)
                    val tmpInput = ByteArrayInputStream(headerData)
                    val header = ObjectInputStream(tmpInput).readObject() as LogPacketHeader

                    if (header.keepAlive) {
                        continue
                    }

                    val data = input.readNBytes(header.length)

                    if (header.source == LogSource.OUT) {
                        System.out.print("> " + data.toString(StandardCharsets.UTF_8).replace(RE_MATCH_NEWLINE, "\n> "))

                    } else if (header.source == LogSource.ERR) {
                        System.err.print("> " + data.toString(StandardCharsets.UTF_8).replace(RE_MATCH_NEWLINE, "\n> "))
                    }

                } catch (e: SocketException) {
                    if (e.message == "Socket closed") {
                        return@launch
                    }

                    throw e
                }
            }
        }

        return LogReceiverInfo(job, input, reader, socket)
    }

    private fun getLogReceiver(
        config: RemoteJUnitConfig?,
        scope: CoroutineScope,
        host: String,
        port: Int
    ): LogReceiverInfo {
        if (config?.reuseContainer == true) {
            if (globalLogServer == null) {
                globalLogServer = startLogReceiver(scope, host, port)
            }

            return globalLogServer as LogReceiverInfo
        }

        return startLogReceiver(scope, host, port)
    }

    private fun createJUnitService(host: String, port: Int): JUnitService {
        val registry = LocateRegistry.getRegistry(host, port)
        return registry.lookup("JUnitTestService") as JUnitService
    }

    private fun getJUnitService(config: RemoteJUnitConfig?, host: String, port: Int): JUnitService {
        if (config?.reuseContainer == true) {
            if (globalJUnitService == null) {
                globalJUnitService = createJUnitService(host, port)
            }

            return globalJUnitService as JUnitService
        }

        return createJUnitService(host, port)
    }

    private fun destroyContainerInfo(
        config: RemoteJUnitConfig?,
        info: ContainerInfo,
        destrMethod: Method,
        force: Boolean = false
    ) {
        if (config?.reuseContainer == true && !force) {
            return
        }

        destrMethod.invoke(null, info)
    }

    private fun stopLogReceiver(config: RemoteJUnitConfig?, info: LogReceiverInfo, force: Boolean = false) {
        if (config?.reuseContainer == true && !force) {
            return
        }

        info.job.cancel("Test execution finished!")
        info.reader.close()
        info.input.close()
        info.socket.close()
    }

}

class LogReceiverInfo(val job: Job, val input: InputStream, val reader: BufferedReader, val socket: Socket)
