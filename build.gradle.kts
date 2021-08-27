import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.30"
    `maven-publish`
}

group = "de.derteufelqwe.junitInDocker"
version = "1.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven { setUrl("https://plugins.gradle.org/m2/") }
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.21")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1-native-mt")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.30")
    // https://mvnrepository.com/artifact/junit/junit
    implementation("junit:junit:4.13.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
    // Exclude the example tests, which don't actually test anything
    exclude("de/derteufelqwe/example/**")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group as String
            artifactId = "junitInDocker"
            version = version

            from(components["java"])

            pom {
                name.set("JUnit-Docker")
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
}

