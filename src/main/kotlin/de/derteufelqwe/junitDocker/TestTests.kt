package de.derteufelqwe.junitDocker

import org.junit.Test


class TestTests {

    fun testTrue() {
        println("Ich bin testTrue")
    }

    fun testFalse() {
        println("Ich bin testFalse")
        throw RuntimeException("Fucked up shit man")
    }
}