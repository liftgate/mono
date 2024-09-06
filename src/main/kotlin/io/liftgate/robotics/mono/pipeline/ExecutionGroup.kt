package io.liftgate.robotics.mono.pipeline

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KClass

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
interface ExecutionGroup : ID, Executable
{
    val localLock: ReentrantReadWriteLock

    val members: MutableList<SingleOrGroupExecution>
    val contextProviders: MutableMap<KClass<out StageContext>, (ExecutionMetadata) -> Any>

    operator fun plusAssign(member: SingleOrGroupExecution)
    {
        localLock.write {
            members += member
        }
    }

    fun backtrack(times: Int)
    {
        throw BacktrackException(times)
    }

    override fun execute(level: Int, metadata: ExecutionMetadata)
    {
        localLock.read {
            var currentIndex = 0
            val endIndex = members.size - 1
            while (currentIndex <= endIndex && !metadata.containsKey("terminate"))
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
                        metadata.parent.terminateMidExecution()
                        failure.printStackTrace()
                        return
                    }
                }.onSuccess {
                    currentIndex += 1
                }
            }
        }
    }
}

inline fun <reified T : StageContext> ExecutionGroup.provides(metadata: ExecutionMetadata): T =
    contextProviders[T::class]!!.invoke(metadata) as T
