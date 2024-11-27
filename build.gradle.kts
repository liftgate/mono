import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `maven-publish`
    java
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "io.liftgate.robotics"
version = "8.2-R1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("RobotCore"))
    compileOnly("com.google.code.gson:gson:2.10.1")
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
        compilerOptions {
            javaParameters = true
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
}

publishing {
    repositories {
        mavenLocal()
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
