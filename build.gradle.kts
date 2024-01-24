import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `maven-publish`
    java
    kotlin("jvm") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "io.liftgate.robotics.mono"
version = "2.9-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("RobotCore"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

tasks {
    withType<ShadowJar> {
        archiveClassifier.set("")
        archiveFileName.set(
            "mono-${project.name}.jar"
        )
    }

    withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
        options.fork()
        options.encoding = "UTF-8"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            // for compat purposes
            javaParameters = true
            jvmTarget = "1.8"
        }
    }
}

publishing {
    repositories {
        configureLiftgateRepository()
    }

    publications {
        register(
            name = "mavenJava",
            type = MavenPublication::class,
            configurationAction = shadow::component
        )
    }
}

fun RepositoryHandler.configureLiftgateRepository()
{
    val contextUrl = runCatching {
        property("liftgate_artifactory_contextUrl")
    }.getOrNull() ?: run {
        println("Skipping Artifactory configuration.")
        return
    }

    maven("$contextUrl/opensource") {
        name = "liftgate"

        credentials {
            username = property("liftgate_artifactory_user").toString()
            password = property("liftgate_artifactory_password").toString()
        }
    }
}
