package io.liftgate.robotics.mono.pipeline

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
interface ExecutionGroup : ID, Executable
{
    val members: List<SingleOrGroupExecution>
    override fun execute(level: Int, metadata: ExecutionMetadata)
    {
        members.forEach {
            it.timedExecution(metadata, level + 1)
        }
    }
}
