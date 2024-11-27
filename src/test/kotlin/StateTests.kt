import io.liftgate.robotics.mono.states.StateHolder
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
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
            variable
        },
        { one, two ->
            variable += 1
            println("Variable: $variable")
            one == two
        }
    )


    var fastVariable = 0
    val fastState by state(
        {
            fastVariable = 0
        },
        {
            fastVariable
        },
        { one, two ->
            fastVariable += 1
            println("Fast variable: $fastVariable")
            one == two
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
        state.override(10)
            .thenCompose {
                println("Completed stage 1!")
                fastState.override(15)
            }
            .thenComposeAsync {
                CompletableFuture.allOf(
                    fastState.override(5),
                    state.override(10)
                )
            }
            .thenAccept {
                println("Completed!")
                completion = true
            }
            .exceptionally {
                it.printStackTrace()
                return@exceptionally null
            }
            .join()
    }
}
