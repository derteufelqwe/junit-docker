package de.derteufelqwe.junit4Docker

import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject

object RMIServer {

    @JvmStatic
    fun main(args: Array<String>) {
        val server = JUnitServiceImpl()
        val stub = UnicastRemoteObject.exportObject(server, 0) as JUnitService
        println("Creating registry")

        val registry: Registry = LocateRegistry.createRegistry(1099)
        registry.rebind("JUnitTestService", stub)


        server.redirectConsole()
        try {
            server.startLogServer()

        } finally {
            server.restoreConsole()
        }

    }


}