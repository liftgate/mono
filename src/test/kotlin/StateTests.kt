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
            variable = it / 2
        },
        {
            variable += 1
            println("Variable: $variable")
            variable
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
                Thread.sleep(1000L)
            }
        }

        println("deploying...")
        state.deploy(10)
            ?.thenAccept {
                println("Completed!")
                completion = true
            }
            ?.join()
    }
}
