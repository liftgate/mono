package io.liftgate.robotics.mono.pipeline

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
class ExecutionMetadata : MutableMap<String, Any> by mutableMapOf()
{
    inline fun <reified T> require(id: String) = this[id] as T
}
