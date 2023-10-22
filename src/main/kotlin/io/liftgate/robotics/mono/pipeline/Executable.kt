package io.liftgate.robotics.mono.pipeline

import io.liftgate.robotics.mono.Mono
import kotlin.system.measureTimeMillis

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
interface Executable : ID
{
    fun execute(level: Int, metadata: ExecutionMetadata)
    fun timedExecution(metadata: ExecutionMetadata, level: Int = 0)
    {
        Mono.logSink("${" ".repeat(level)}[${id()}] Starting execution")
        Mono.logSink("${" ".repeat(level)}[${id()}] Completed execution in ${
            measureTimeMillis { execute(level, metadata) }
        }ms.")
    }
}
