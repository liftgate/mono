package io.liftgate.robotics.mono.pipeline

import io.liftgate.robotics.mono.Mono
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
class RootExecutionGroup : ExecutionGroup
{
    override val localLock = ReentrantReadWriteLock()
    private val metadata = ExecutionMetadata(this)
    override val members = mutableListOf<SingleOrGroupExecution>()

    override val contextProviders = mutableMapOf<KClass<out StageContext>, (ExecutionMetadata) -> Any>()

    inline fun <reified T : StageContext> providesContext(
        crossinline builder: () -> T
    )
    {
        contextProviders[T::class] = { builder() }
    }

    inline fun <reified T : StageContext> providesContext(
        crossinline builder: (ExecutionMetadata) -> T
    )
    {
        contextProviders[T::class] = { builder(it) }
    }

    fun executeBlocking()
    {
        timedExecution(this.metadata)
    }

    private var hasCalledTermination = false
    private val terminationLock = ReentrantLock()

    /**
     * Allow the program to exit mid-process without having to do
     * multithreaded magic and messing with Thread#interrupt.
     */
    fun terminateMidExecution() = terminationLock.withLock {
        if (hasCalledTermination)
        {
            return@withLock
        }

        metadata["terminate"] = true
    }

    fun describe()
    {
        fun recur(level: Int, member: SingleOrGroupExecution)
        {
            if (member.isSingleExecution())
            {
                Mono.logSink("${" ".repeat(level)}| ${member.id()}")
                return
            }

            Mono.logSink("${" ".repeat(level)}- ${member.id()}")

            member.group!!.members.forEach {
                recur(level + 1, it)
            }
        }

        for (member in members)
        {
            recur(0, member)
        }
    }

    override fun id() = "__root__"
}
