package de.derteufelqwe.junitInDocker

class RemoteClassLoader(var bytes: ByteArray = ByteArray(0)) : ClassLoader() {


    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String): Class<*> {
        return super.loadClass(name)
    }

    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*> {
        if (bytes.isNotEmpty()) {
            val res = defineClass(name, bytes, 0, bytes.size)
            bytes = ByteArray(0)
            return res

        } else {
            return super.findClass(name)
        }
    }

}