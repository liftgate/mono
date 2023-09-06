import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `maven-publish`
    kotlin("jvm") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "io.liftgate.robotics.mono"
version = "1.0-SNAPSHOT"

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
    jvmToolchain(11)
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    archiveFileName.set(
        "on-bot-kotlin-${project.name}.jar"
    )
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        // for compat purposes
        javaParameters = true
        jvmTarget = "11"
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