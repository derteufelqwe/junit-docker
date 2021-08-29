package de.derteufelqwe.junitDocker

import sun.rmi.server.UnicastRef
import sun.rmi.transport.LiveRef
import sun.rmi.transport.tcp.TCPEndpoint
import java.lang.reflect.Proxy
import java.rmi.registry.LocateRegistry
import java.rmi.server.RemoteObjectInvocationHandler


fun main() {
    val registy = LocateRegistry.getRegistry(1099)
    val service = registy.lookup("JUnitTestService") as JUnitService

    val invocationHandler = Proxy.getInvocationHandler(service) as RemoteObjectInvocationHandler
    val ref = invocationHandler.ref as UnicastRef
    val liveRef = ref.liveRef as LiveRef

    val epF = LiveRef::class.java.getDeclaredField("ep")
    epF.isAccessible = true
    val ep = epF.get(liveRef)

    val portF = TCPEndpoint::class.java.getDeclaredField("port")
    portF.isAccessible = true
    val hostF = TCPEndpoint::class.java.getDeclaredField("host")
    hostF.isAccessible = true

    // portF.setInt(ep, 1000)
    // hostF.set(ep, "ubuntu1")
}