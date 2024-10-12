package io.liftgate.robotics.mono.konfig

import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import net.mamoe.yamlkt.YamlBuilder
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import java.io.File
import kotlin.reflect.KClass

val yaml = Yaml {
    stringSerialization = YamlBuilder.StringSerialization.SINGLE_QUOTATION
    encodeDefaultValues = true
    nullSerialization = YamlBuilder.NullSerialization.NULL
    mapSerialization = YamlBuilder.MapSerialization.BLOCK_MAP
}

inline fun <reified T : Any> konfig(configure: Konfig<T>.() -> Unit = { }) = Konfig(
    T::class,
    defaultCreator = { T::class.java.newInstance() },
    existingCreator = {
        yaml.decodeFromString(it)
    },
    existingPersist = {
        yaml.encodeToString(it)
    }
).apply(configure).apply { start() }

/**
 * @author GrowlyX
 * @since 9/14/2024
 */
class Konfig<T : Any>(
    private val type: KClass<T>,
    private var name: String = type.simpleName?.lowercase() ?: "unknown",
    private val defaultCreator: () -> T,
    private val existingCreator: (String) -> T,
    private val existingPersist: (T) -> String
)
{
    private val configPath by lazy {
        if (local)
        {
            return@lazy File("konfig", "$name.yml")
        }

        AppUtil.getInstance()
            .getSettingsFile("$name.yml")
    }

    private lateinit var cached: T
    private var started = false
    private var local = false

    fun start()
    {
        if (started)
        {
            throw IllegalStateException("Konfig instance is already started")
        }

        load()
        started = true
    }

    fun withCustomFileID(name: String)
    {
        this.name = name
    }

    fun local()
    {
        this.local = true
    }

    fun get() = cached

    private fun load()
    {
        if (!configPath.exists())
        {
            cached = defaultCreator()
            configPath.createNewFile()
            configPath.writeText(existingPersist(cached))
            return
        }

        val existing = existingCreator(configPath.readText())
        cached = existing
    }
}
