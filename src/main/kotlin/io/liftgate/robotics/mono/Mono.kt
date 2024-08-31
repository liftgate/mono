package io.liftgate.robotics.mono

import com.qualcomm.robotcore.hardware.Gamepad
import io.liftgate.robotics.mono.gamepad.GamepadCommands
import io.liftgate.robotics.mono.pipeline.RootExecutionGroup
import io.liftgate.robotics.mono.states.StateHolder
import java.util.concurrent.Executors

/**
 * @author GrowlyX
 * @since 9/5/2023
 */
object Mono
{
    @JvmStatic
    val COMMANDS = Executors.newSingleThreadScheduledExecutor()

    @JvmStatic
    val EXECUTION = Executors.newSingleThreadScheduledExecutor()

    var logSink = { message: String -> println(message) }

    fun commands(gamepad: Gamepad) = GamepadCommands(gamepad)
    fun buildExecutionGroup(block: RootExecutionGroup.() -> Unit) =
        RootExecutionGroup().apply(block)
}
