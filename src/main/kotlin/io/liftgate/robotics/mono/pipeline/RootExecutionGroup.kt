package io.liftgate.robotics.mono.pipeline

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
class RootExecutionGroup(
    override val members: List<SingleOrGroupExecution>
) : ExecutionGroup
{
    private val metadata = ExecutionMetadata()

    fun execute()
    {
        timedExecution(this.metadata)
    }

    fun describe()
    {
        fun recur(level: Int, member: SingleOrGroupExecution)
        {
            if (member.isSingleExecution())
            {
                println("${" ".repeat(level)}| ${member.id()}")
                return
            }

            println("${" ".repeat(level)}- ${member.id()}")

            member.group!!.members.forEach {
                recur(level + 1, it)
            }
        }

        for (member in members)
        {
            recur(0, member)
        }
    }

    override fun id() = "__root__"
}
