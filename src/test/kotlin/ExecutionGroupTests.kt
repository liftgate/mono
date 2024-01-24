import io.liftgate.robotics.mono.Mono
import io.liftgate.robotics.mono.pipeline.StageContext
import io.liftgate.robotics.mono.pipeline.simultaneous
import io.liftgate.robotics.mono.pipeline.single
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
class ExecutionGroupTests
{
    enum class TapeSide
    {
        Left, Middle, Right
    }

    @Test
    fun testContextualExecution()
    {
        class DemoStageContext : StageContext
        {
            var isComplete = false

            override fun requiresAtLeast() = TimeUnit.SECONDS.toMillis(1L)
            override fun isCompleted() = isComplete
            override fun timesOutAfter() = 2000L
        }

        val group = Mono.buildExecutionGroup {
            providesContext(::DemoStageContext)

            single<DemoStageContext>("!! demo stage context with 1s minimum, 2s maximum (should wait 1s) !!")
            {
                Thread.sleep(500L)
                isComplete = true

                terminateMidExecution()

                println("this should work, but we'll move on in 500ms!")
            }

            single<DemoStageContext>("!! demo stage context with 1s minimum, 2s maximum (should not time out) !!")
            {
                Thread.sleep(1500L)
                isComplete = true

                println("this SHOULD work!")
            }

            single<DemoStageContext>("!! demo stage context with 1s minimum, 2s maximum (should time out) !!")
            {
                Thread.sleep(3000L)
                isComplete = true

                println("this shouldn't work!")
            }
        }

        group.describe()
        group.executeBlocking()
    }

    @Test
    @Disabled
    fun test()
    {
        val group = Mono.buildExecutionGroup {
            single("detect team element") {
                // TODO
                Thread.sleep(1000L)
                // do some detection logic and find the tape side
                put("tape", TapeSide.Left)
            }

            // executed simultaneously
            simultaneous(
                "move and lower elevator"
            ) {
                single("move towards tape") {
                    Thread.sleep(1000L)
                    // TODO
                }

                single("lower elevator") {
                    Thread.sleep(1000L)
                    // TODO
                }
            }
            single("turn towards tape") {
                // get the tape side which we calculated from the first step
                val tapeSide = require<TapeSide>("tape")
            }
            single("deposit yellow and purple pixel") {
                Thread.sleep(1000L)
                // TODO
            }
            single("elevate elevator") {
                Thread.sleep(1000L)
                // TODO
            }
            single("grab yellow pixel") {
                Thread.sleep(1000L)
                // TODO
            }
            simultaneous(
                "turn towards side bar while elevating claw",
            ) {
                single("turn towards side bar/backdrop") {
                    Thread.sleep(1000L)
                    // TODO
                }

                single("elevate claw") {
                    Thread.sleep(1000L)
                    // TODO
                }
            }

            // ---- depends on position of robot
            single("go forward") {
                Thread.sleep(1000L)
                // TODO
            }
            // ----

            // TODO: do we even need to detect the april tags
            simultaneous(
                "strafe into position while elevating elevator"
            ) {
                single("strafe into position") {
                    // align with thing
                    val tapeSide = require<TapeSide>("tape")
                    Thread.sleep(1000L)
                    // TODO
                }

                single("elevate claw") {
                    Thread.sleep(1000L)
                    // TODO
                }
            }

            single("deposit yellow pixel") {
                Thread.sleep(1000L)
                // TODO
            }
        }

        Mono.logSink("-".repeat(25))
        Mono.logSink("Describing")
        Mono.logSink("-".repeat(25))
        group.describe()
        Mono.logSink("-".repeat(25))
        Mono.logSink("Executing")
        Mono.logSink("-".repeat(25))
        group.executeBlocking()
        Mono.logSink("-".repeat(25))
    }
}
