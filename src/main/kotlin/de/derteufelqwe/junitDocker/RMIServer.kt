package de.derteufelqwe.junitDocker

object RMIServer {

    @JvmStatic
    fun main(args: Array<String>) {
        JUnitDockerServer().startAndAwait()
    }


}