package io.liftgate.robotics.mono.pipeline

import io.liftgate.robotics.mono.Mono
import kotlin.reflect.KClass

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
class RootExecutionGroup : ExecutionGroup
{
    private val metadata = ExecutionMetadata()
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
