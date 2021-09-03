package de.derteufelqwe.example;

import de.derteufelqwe.junitDocker.DockerRunner;
import de.derteufelqwe.junitDocker.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DockerRunner.class)
@RequiredClasses(value = {Dep1.class})
@RemoteJUnitConfig(reuseContainer = true, logReceiveDelay = 200)
public class ExampleTests {

    @ContainerProvider
    public static ContainerInfo provideContainer() {
//        return new ContainerInfo("localhost", 1099, 9876);
        return new ContainerInfo("localhost", 5000, 5001);
    }

    @ContainerDestroyer
    public static void destroyContainer(ContainerInfo info) {

    }

    @Test
    public void testSuccess() {
        System.out.println("I am a working test");
        System.out.println("I am a working test2");
    }

    @Test
    public void testFailure() {
        System.err.println("I am going to fail");
//        throw new RuntimeException("Oh no! Failure.");
    }

}
