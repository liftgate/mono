package io.liftgate.robotics.mono

import com.qualcomm.robotcore.hardware.Gamepad
import io.liftgate.robotics.mono.gamepad.GamepadCommands
import io.liftgate.robotics.mono.pipeline.RootExecutionGroup

/**
 * @author GrowlyX
 * @since 9/5/2023
 */
object Mono
{
    var logSink = { message: String -> println(message) }

    fun commands(gamepad: Gamepad) = GamepadCommands(gamepad)
    fun buildExecutionGroup(block: RootExecutionGroup.() -> Unit) =
        RootExecutionGroup().apply(block)
}
