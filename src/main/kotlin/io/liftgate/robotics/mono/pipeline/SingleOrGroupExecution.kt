package io.liftgate.robotics.mono.pipeline

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
interface SingleOrGroupExecution : ID, Executable
{
    val single: SingleExecution?
    val group: ExecutionGroup?

    fun isGroupExecution() = group != null
    fun isSingleExecution() = single != null

    override fun id() = when (true)
    {
        (single != null) -> single!!.id()
        (group != null) -> group!!.id()
        else -> throw IllegalStateException(
            "Both single and group executions are null!"
        )
    }

    override fun execute(level: Int, metadata: ExecutionMetadata) = when (true)
    {
        (single != null) -> single!!.execute(level, metadata)
        (group != null) -> group!!.execute(level, metadata)
        else -> throw IllegalStateException(
            "Both single and group executions are null!"
        )
    }
}
