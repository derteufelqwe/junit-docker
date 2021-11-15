package de.derteufelqwe.junitDocker.util

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiredClasses(val value: Array<KClass<*>>)


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ContainerProvider()


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ContainerDestroyer()


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RemoteJUnitConfig(
    val reuseContainer: Boolean = false,      // Run all tests in one container. False = new container for each test
    val containerStartupDelay: Long = 1000,   // Time to wait after the container started
    val logReceiveDelay: Long = 100,          // Time to wait after a test to receive the remaining logs
    val useCache: Boolean = true,             // Caches test classes and their dependencies if required
)


data class ContainerInfo(val host: String, val containerID: String, val rmiPort: Int, val logPort: Int,
                         val infos: MutableMap<String, Any>) {
    // Default value not working when calling from java
    constructor(host: String, containerID: String, rmiPort: Int, logPort: Int) : this(host, containerID, rmiPort, logPort, mutableMapOf())
}
