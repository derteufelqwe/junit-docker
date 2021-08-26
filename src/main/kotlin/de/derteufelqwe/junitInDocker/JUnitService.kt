package de.derteufelqwe.junitInDocker

import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException
import java.util.*

interface JUnitService : Remote {

    @Throws(RemoteException::class)
    fun executeTest(classHash: Int, instanceId: UUID, className: String, method: String)

    @Throws(RemoteException::class)
    fun loadRequiredClasses(mainClass: ClassInfo, dependClasses: List<ClassInfo>, hash: Int)

    @Throws(RemoteException::class)
    fun isClassCached(hash: Int): Boolean

}

class ClassInfo(val name: String, val data: ByteArray) : Serializable