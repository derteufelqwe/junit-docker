import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.30"
    id("maven-publish")
}

group = "de.derteufelqwe.junit-docker"
version = "1.0"

val mainClass = "de.derteufelqwe.junitDocker.RMIServer"

repositories {
    mavenCentral()
    mavenLocal()
    maven { setUrl("https://plugins.gradle.org/m2/") }
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.21")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.30")
    // https://mvnrepository.com/artifact/junit/junit
    implementation("junit:junit:4.13.2")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.21")
}

tasks.test {
    useJUnit()
    // Exclude the example tests, which don't actually test anything
    if (System.getenv("GITHUB_WORKFLOW") != null) {
        println("Excluding example tests for Github CI build")
        exclude("de/derteufelqwe/example/**")
    }
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.jar {
    manifest {
        attributes("Main-Class" to mainClass)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group as String
            artifactId = "junit-docker"
            version = version

            from(components["java"])

            pom {
                name.set("JUnit-Dockerxx")
                description.set("Run JUnit4 tests inside of docker containers or on remote hosts")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("derteufelqwe")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/derteufelqwe/junit-docker")
            credentials {
                username = "derteufelqwe"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

}


// Task to get current version of the project.
abstract class ArtifactNameTask : DefaultTask() {

    @org.gradle.api.tasks.TaskAction
    fun printArtifactName() {
        println("${project.name}-${project.version}.jar")
    }
}

tasks.register<ArtifactNameTask>("artifactName")


tasks {
    register("fatJar", Jar::class.java) {
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to mainClass)
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
}
