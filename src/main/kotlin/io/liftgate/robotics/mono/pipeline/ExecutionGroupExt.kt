package io.liftgate.robotics.mono.pipeline

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
internal fun wrap(group: ExecutionGroup) = object : SingleOrGroupExecution
{
    override val single: SingleExecution? = null
    override val group: ExecutionGroup = group
}

internal fun wrap(single: SingleExecution) = object : SingleOrGroupExecution
{
    override val single: SingleExecution = single
    override val group: ExecutionGroup? = null
}

fun single(id: String, lambda: ExecutionMetadata.() -> Unit) = wrap(object : SingleExecution
{
    override fun execute(level: Int, metadata: ExecutionMetadata) = lambda(metadata)
    override fun id() = id
})

fun parallel(id: String, vararg executions: SingleOrGroupExecution) =
    wrap(object : ParallelExecutionGroup
    {
        override val members: List<SingleOrGroupExecution> = executions.toList()
        override fun id() = id
    })
