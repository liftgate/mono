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

    fun backtrack(times: Int)
    {
        throw BacktrackException(times)
    }

    override fun execute(level: Int, metadata: ExecutionMetadata)
    {
        var currentIndex = 0
        val endIndex = members.size - 1
        while (currentIndex <= endIndex)
        {
            val currentMember = members[currentIndex]
            kotlin.runCatching {
                currentMember.timedExecution(metadata, level + 1)
            }.onFailure { failure ->
                if (failure is BacktrackException)
                {
                    currentIndex -= failure.amount.coerceAtMost(currentIndex)
                } else
                {
                    failure.printStackTrace()
                    return
                }
            }.onSuccess {
                currentIndex += 1
            }
        }
    }
}

inline fun <reified T : StageContext> ExecutionGroup.provides(metadata: ExecutionMetadata): T =
    contextProviders[T::class]!!.invoke(metadata) as T
