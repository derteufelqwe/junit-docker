package de.derteufelqwe.junitInDocker.util

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
    val reuseContainer: Boolean = false,   // Run all tests in one container. False = new container for each test
    val containerStartupDelay: Long = 1000,   // Time to wait after the container started
    val logReceiveDelay: Long = 100,     // Time to wait after a test to receive the remaining logs
)


data class ContainerInfo(val host: String, val rmiPort: Int, val logPort: Int)