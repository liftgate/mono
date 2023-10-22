package io.liftgate.robotics.mono.pipeline

import kotlin.reflect.KClass

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
internal fun wrap(group: ExecutionGroup) = object : SingleOrGroupExecution
{
    override val single: SingleExecution? = null
    override val group: ExecutionGroup = group
}

fun wrap(single: SingleExecution) = object : SingleOrGroupExecution
{
    override val single: SingleExecution = single
    override val group: ExecutionGroup? = null
}

inline fun <reified T : StageContext> ExecutionGroup.single(id: String, crossinline lambda: T.(ExecutionMetadata) -> Unit) = with(wrap(object : SingleExecutionWithContext<T>
{
    override fun provideContext(metadata: ExecutionMetadata) = provides<T>(metadata)
    override fun executeAsync(level: Int, metadata: ExecutionMetadata, context: T) = lambda(context, metadata)
    override fun id() = id
})) {
    this@single += this
}

fun ExecutionGroup.single(id: String, lambda: ExecutionMetadata.() -> Unit) = with(wrap(object : SingleExecution
{
    override fun execute(level: Int, metadata: ExecutionMetadata) = lambda(metadata)
    override fun id() = id
})) {
    this@single += this
}

fun ExecutionGroup.parallel(id: String, block: ParallelExecutionGroup.() -> Unit) = with(wrap(object : ParallelExecutionGroup
{
    override val members: MutableList<SingleOrGroupExecution> = mutableListOf()
    override fun id() = id

    override val contextProviders = this@parallel.contextProviders

    init
    {
        block()
    }
})) {
    this@parallel += this
}
