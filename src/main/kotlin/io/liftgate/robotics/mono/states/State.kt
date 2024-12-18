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

    fun inProgress() = currentJob != null

    internal fun periodic() = lock.read {
        runCatching {
            val current = read()
            this.current = current

            additionalPeriodic(current, target)

            if (currentJob == null)
            {
                return@runCatching
            }

            if (complete(current, target!!))
            {
                kotlin.runCatching {
                    currentJob?.complete(StateResult.Success)
                }.onFailure {
                    it.printStackTrace()
                }
                currentJob = null
                target = null
            } else if (currentJobTimeOut != 0L)
            {
                if (System.currentTimeMillis() - currentJobStart > currentJobTimeOut)
                {
                    kotlin.runCatching {
                        currentJob?.complete(StateResult.Timeout)
                    }.onFailure {
                        it.printStackTrace()
                    }
                    currentJob = null
                    target = null
                }
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun deploy(newValue: T, timeout: Long = 0L): CompletableFuture<StateResult>? = lock.write {
        if (currentJob != null)
        {
            return null
        }

        write(newValue)

        currentJob = CompletableFuture()
        target = newValue
        currentJobStart = System.currentTimeMillis()
        currentJobTimeOut = timeout
        return currentJob!!
    }

    fun override(newValue: T, timeout: Long = 0L): CompletableFuture<StateResult> {
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
