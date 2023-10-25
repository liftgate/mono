package io.liftgate.robotics.mono.subsystem

import io.liftgate.robotics.mono.pipeline.StageContext

/**
 * @author GrowlyX
 * @since 10/25/2023
 */
interface Subsystem : StageContext
{
    fun initialize()
    fun composeStageContext(): StageContext
}
