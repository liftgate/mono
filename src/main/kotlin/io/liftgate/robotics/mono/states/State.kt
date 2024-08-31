package io.liftgate.robotics.mono.states

import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * @author GrowlyX
 * @since 8/31/2024
 */
class State<T : Any>(private val write: (T) -> Unit, private val read: () -> T, private val complete: (T, T) -> Boolean = { one, two -> one == two })
{
    private var target: T? = null
    private var currentJob: CompletableFuture<Void>? = null
    private val lock = ReentrantReadWriteLock()

    fun inProgress() = lock.read { currentJob != null }

    internal fun periodic() = lock.read {
        if (currentJob == null)
        {
            return
        }

        if (complete(read(), target!!))
        {
            currentJob?.complete(null)
            currentJob = null
            target = null
        }
    }

    fun deploy(newValue: T): CompletableFuture<Void>? = lock.write {
        if (currentJob != null)
        {
            return null
        }

        currentJob = CompletableFuture()
        target = newValue

        write(newValue)
        return currentJob!!
    }

    fun override(newValue: T): CompletableFuture<Void> = lock.write {
        if (currentJob != null)
        {
            reset()
        }

        return deploy(newValue)!!
    }

    fun reset() = lock.write {
        currentJob?.completeExceptionally(StateCancelException())
        currentJob = null
        target = null
    }
}
