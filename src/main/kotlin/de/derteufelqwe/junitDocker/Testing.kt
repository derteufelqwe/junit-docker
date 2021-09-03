package de.derteufelqwe.junitDocker

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import sun.rmi.server.UnicastRef
import sun.rmi.transport.LiveRef
import sun.rmi.transport.tcp.TCPEndpoint
import java.lang.reflect.Proxy
import java.rmi.registry.LocateRegistry
import java.rmi.server.RemoteObjectInvocationHandler


fun main() {
    runBlocking {
        launch {
            println("Launched")
            throw RuntimeException("YO")
        }
    }
}