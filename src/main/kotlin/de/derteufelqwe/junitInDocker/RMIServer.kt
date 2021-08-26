package de.derteufelqwe.junitInDocker

object RMIServer {

    @JvmStatic
    fun main(args: Array<String>) {
        JUnitDockerServer().startAndAwait()
    }


}