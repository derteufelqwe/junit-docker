package de.derteufelqwe.junitInDocker

import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject

class JUnitDockerServer(val rmiPort: Int = 1099) {

    val server = JUnitServiceImpl()
    val stub = UnicastRemoteObject.exportObject(server, rmiPort) as JUnitService


    fun startAndAwait() {
        println("Creating registry")
        val registry: Registry = LocateRegistry.createRegistry(rmiPort)
        registry.rebind("JUnitTestService", stub)

        server.redirectConsole()
        try {
            server.startLogServer()

        } finally {
            server.restoreConsole()
        }
    }

}