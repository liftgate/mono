package io.liftgate.robotics.mono.subsystem

import io.liftgate.robotics.mono.states.StateHolder
import io.liftgate.robotics.mono.subsystem.terminable.composite.CompositeTerminable

/**
 * @author GrowlyX
 * @since 11/9/2023
 */
abstract class AbstractSubsystem : Subsystem, StateHolder(), CompositeTerminable by CompositeTerminable.create()
{
    abstract fun doInitialize()

    override fun initialize()
    {
        doInitialize()
        with {
            this.dispose()
        }
    }
}
