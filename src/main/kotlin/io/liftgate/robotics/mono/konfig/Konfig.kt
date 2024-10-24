package io.liftgate.robotics.mono.konfig

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import java.io.File
import kotlin.reflect.KClass

inline fun <reified T : Any> konfig(configure: Konfig<T>.() -> Unit = { }) = Konfig(
    T::class,
    defaultCreator = { T::class.java.newInstance() },
    existingCreator = { yaml, contents -> yaml.fromJson(contents, T::class.java) }
).apply(configure).apply { start() }

/**
 * @author GrowlyX
 * @since 9/14/2024
 */
class Konfig<T : Any>(
    private val type: KClass<T>,
    private var name: String = type.simpleName?.lowercase() ?: "unknown",
    private val defaultCreator: () -> T,
    private val existingCreator: (Gson, String) -> T,
)
{
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

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
        val configPath = if (local)
        {
            File("konfig", "$name.json")
        } else
        {
            AppUtil.getInstance().getSettingsFile("$name.json")
        }

        if (!configPath.exists())
        {
            cached = defaultCreator()
            configPath.createNewFile()
            configPath.writeText(gson.toJson(cached))
            return
        }

        val existing = existingCreator(gson, configPath.readText())
        cached = existing
    }
}
