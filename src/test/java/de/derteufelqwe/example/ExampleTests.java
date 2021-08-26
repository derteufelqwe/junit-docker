package de.derteufelqwe.example;

import de.derteufelqwe.junit4Docker.DockerRunner;
import de.derteufelqwe.junit4Docker.util.ContainerDestroyer;
import de.derteufelqwe.junit4Docker.util.ContainerInfo;
import de.derteufelqwe.junit4Docker.util.ContainerProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DockerRunner.class)
public class ExampleTests {

    @ContainerProvider
    public static ContainerInfo provideContainer() {
        return new ContainerInfo("localhost", 1099, 9876);
    }

    @ContainerDestroyer
    public static void destroyContainer(ContainerInfo info) {

    }

    @Test
    public void testSuccess() {
        System.out.println("I am a working test");
    }

    @Test
    public void testFailure() {
        System.err.println("I am going to fail");
        throw new RuntimeException("Oh no! Failure.");
    }

}
