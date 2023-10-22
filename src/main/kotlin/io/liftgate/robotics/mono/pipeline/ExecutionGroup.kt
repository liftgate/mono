package io.liftgate.robotics.mono.pipeline

import kotlin.reflect.KClass

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
interface ExecutionGroup : ID, Executable
{
    val members: MutableList<SingleOrGroupExecution>
    val contextProviders: MutableMap<KClass<out StageContext>, (ExecutionMetadata) -> Any>

    operator fun plusAssign(member: SingleOrGroupExecution)
    {
        members += member
    }

    override fun execute(level: Int, metadata: ExecutionMetadata)
    {
        members.forEach {
            it.timedExecution(metadata, level + 1)
        }
    }
}

inline fun <reified T : StageContext> ExecutionGroup.provides(metadata: ExecutionMetadata): T =
    contextProviders[T::class]!!.invoke(metadata) as T
