package io.liftgate.robotics.mono.pipeline

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
        println("${" ".repeat(level)}[${id()}] Starting execution")
        println("${" ".repeat(level)}[${id()}] Completed execution in ${
            measureTimeMillis { execute(level, metadata) }
        }ms.")
    }
}
