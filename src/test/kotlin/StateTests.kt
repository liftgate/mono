import io.liftgate.robotics.mono.states.StateHolder
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

/**
 * @author GrowlyX
 * @since 9/14/2024
 */
class StateTests : StateHolder()
{
    var variable = 0
    val state by state(
        {
            variable = 0
        },
        {
            variable += 1
            println("Variable: $variable")
            variable
        }
    )

    var variableX = 0
    val state2 by state(
        {
            variableX = 0
        },
        {
            variableX += 1
            println("VariableX: $variableX")
            if (variableX == 11)
            {
                throw NullPointerException("deadlock potential")
            }
            variableX
        }
    )

    @Test
    fun testStateHold()
    {
        var completion = false
        thread {
            while (!completion)
            {
                runCatching {
                    allPeriodic()
                }.onFailure {
                    it.printStackTrace()
                }
                Thread.sleep(50L)
            }
        }

        println("deploying...")
        state.deploy(20)
            ?.thenAccept {
                println("Completed 1!")
            }
            ?.join()

        state2.deploy(15)
            ?.thenAccept {
                println("Completed 2!")
            }
            ?.join()
    }
}
