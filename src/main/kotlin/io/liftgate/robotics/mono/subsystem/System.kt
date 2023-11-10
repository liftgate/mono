package io.liftgate.robotics.mono.subsystem

import io.liftgate.robotics.mono.subsystem.terminable.TerminableConsumer
import io.liftgate.robotics.mono.subsystem.terminable.module.TerminableModule

/**
 * @author GrowlyX
 * @since 11/9/2023
 */
interface System : TerminableModule
{
    val subsystems: MutableSet<Subsystem>
    fun initializeAll() = subsystems
        .onEach(Subsystem::initialize)

    fun disposeOfAll() = subsystems.onEach(
        Subsystem::closeAndReportException
    )

    override fun setup(consumer: TerminableConsumer)
    {
        val subsystem = consumer as Subsystem
        subsystems += subsystem
        subsystem.with { subsystem.dispose() }
    }

    fun register(vararg subsystem: Subsystem)
    {
        subsystem.onEach(::bindModuleWith)
    }
}
