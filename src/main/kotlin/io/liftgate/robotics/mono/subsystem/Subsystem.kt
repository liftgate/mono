package io.liftgate.robotics.mono.subsystem

import io.liftgate.robotics.mono.pipeline.StageContext
import io.liftgate.robotics.mono.subsystem.terminable.composite.CompositeTerminable

/**
 * @author GrowlyX
 * @since 10/25/2023
 */
interface Subsystem : StageContext, CompositeTerminable
{
    fun initialize()
    fun composeStageContext(): StageContext
    {
        throw Error("Un implemented")
    }
}
