package io.liftgate.robotics.mono.pipeline

import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
internal fun __INTERNAL__wrap(group: ExecutionGroup) = object : SingleOrGroupExecution
{
    override val single: SingleExecution? = null
    override val group: ExecutionGroup = group
}

fun __INTERNAL__wrap(single: SingleExecution) = object : SingleOrGroupExecution
{
    override val single: SingleExecution = single
    override val group: ExecutionGroup? = null
}

/**
 * Halts the current thread for [millis] milliseconds.
 */
fun ExecutionGroup.waitMillis(millis: Long) =
    single("waitMillis-$millis") {
        Thread.sleep(millis)
    }

/**
 * Describes a stage with single execution of block [lambda] in
 * context of a new instance of [T].
 */
inline fun <reified T : StageContext> ExecutionGroup.single(
    id: String,
    crossinline lambda: T.(ExecutionMetadata) -> Unit
) = with(__INTERNAL__wrap(object : SingleExecutionWithContext<T>
{
    override fun provideContext(metadata: ExecutionMetadata) = provides<T>(metadata)
    override fun executeAsync(level: Int, metadata: ExecutionMetadata, context: T) = lambda(context, metadata)
    override fun id() = id
})) {
    this@single += this
}

/**
 * Describes a stage with single execution of block [lambda]. Run under the
 * parent execution group provided by [single].
 */
fun ExecutionGroup.single(id: String, lambda: ExecutionMetadata.() -> Unit) =
    with(__INTERNAL__wrap(object : SingleExecution
    {
        override fun execute(level: Int, metadata: ExecutionMetadata) = lambda(metadata)
        override fun id() = id
    })) {
        this@single += this
    }

/**
 * Builds a parallel execution group with an immutable copy of the
 * context provides bound to the parent ParallelExecutionGroup.
 *
 * Allows for consecutive executions of [SingleOrGroupExecution] members ((A1 -> B2) -> C).
 */
fun ParallelExecutionGroup.consecutive(id: String, block: ExecutionGroup.() -> Unit) =
    with(__INTERNAL__wrap(object : ExecutionGroup
    {
        override val localLock = ReentrantReadWriteLock()

        override val members = mutableListOf<SingleOrGroupExecution>()
        override val contextProviders = this@consecutive.contextProviders.toMutableMap()

        override fun id() = id
    }.apply(block))) {
        this@consecutive += this
    }

/**
 * Builds a parallel execution group with an immutable copy
 * of the context provides bound to the previous ExecutionGroup.
 *
 * Allows for parallel execution of [SingleOrGroupExecution] members ((A1, B2) -> C).
 */
fun ExecutionGroup.simultaneous(id: String, block: ParallelExecutionGroup.() -> Unit) =
    with(__INTERNAL__wrap(object : ParallelExecutionGroup
    {
        override val localLock = ReentrantReadWriteLock()

        override val members = mutableListOf<SingleOrGroupExecution>()
        override fun id() = id

        override val contextProviders = this@simultaneous.contextProviders.toMutableMap()
    }.apply(block))) {
        this@simultaneous += this
    }
