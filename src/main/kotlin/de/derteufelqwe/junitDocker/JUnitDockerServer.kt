package de.derteufelqwe.junitDocker

import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import kotlin.system.measureTimeMillis

class JUnitDockerServer(val rmiPort: Int = 1099) {

    fun startAndAwait() {
//        System.setProperty("java.rmi.server.logCalls", "true");
        System.setProperty("java.rmi.server.hostname", "localhost")

        val server = JUnitServiceImpl()
        val stub = UnicastRemoteObject.exportObject(server, rmiPort) as JUnitService

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