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
    private var current: T? = null
    private var target: T? = null

    private var currentJob: CompletableFuture<StateResult>? = null
    private var currentJobStart: Long = System.currentTimeMillis()
    private var currentJobTimeOut: Long = 0L

    private val lock = ReentrantReadWriteLock()
    private var additionalPeriodic: (T, T?) -> Unit = { _, _ -> }

    fun current() = current ?: throw IllegalStateException("State has not been initialized yet")
    fun additionalPeriodic(block: (T, T?) -> Unit)
    {
        this.additionalPeriodic = block
    }

    fun inProgress() = lock.read { currentJob != null }

    internal fun periodic() = lock.read {
        val current = read()
        this.current = current

        additionalPeriodic(current, target)

        if (currentJob == null)
        {
            return
        }

        if (complete(current, target!!))
        {
            currentJob?.complete(StateResult.Success)
            currentJob = null
            target = null
        } else if (currentJobTimeOut != 0L)
        {
            if (System.currentTimeMillis() - currentJobStart > currentJobTimeOut)
            {
                currentJob?.complete(StateResult.Timeout)
                currentJob = null
                target = null
            }
        }
    }

    fun deploy(newValue: T, timeout: Long = 0L): CompletableFuture<StateResult>? = lock.write {
        if (currentJob != null)
        {
            return null
        }

        currentJob = CompletableFuture()
        target = newValue
        currentJobStart = System.currentTimeMillis()
        currentJobTimeOut = timeout

        write(newValue)
        return currentJob!!
    }

    fun override(newValue: T, timeout: Long = 0L): CompletableFuture<StateResult> = lock.write {
        if (currentJob != null)
        {
            reset()
        }

        return deploy(newValue, timeout)!!
    }

    fun reset() = lock.write {
        currentJob?.completeExceptionally(StateCancelException())
        currentJob = null
        target = null
    }
}
