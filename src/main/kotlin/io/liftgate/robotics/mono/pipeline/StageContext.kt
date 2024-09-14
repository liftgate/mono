package io.liftgate.robotics.mono.pipeline

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
interface StageContext
{
    fun isCompleted(): Boolean
    {
        return false
    }

    fun timesOutAfter(): Long?
    {
        return null
    }

    fun requiresAtLeast(): Long?
    {
        return null
    }

    fun dispose()
    {

    }
}
