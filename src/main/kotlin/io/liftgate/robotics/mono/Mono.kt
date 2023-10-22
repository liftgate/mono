package io.liftgate.robotics.mono

import com.qualcomm.robotcore.hardware.Gamepad
import io.liftgate.robotics.mono.gamepad.GamepadCommands
import io.liftgate.robotics.mono.pipeline.RootExecutionGroup
import io.liftgate.robotics.mono.pipeline.SingleOrGroupExecution

/**
 * @author GrowlyX
 * @since 9/5/2023
 */
object Mono
{
    var logSink = { message: String -> println(message) }

    fun commands(gamepad: Gamepad) = GamepadCommands(gamepad)
    fun executionGroup(vararg members: SingleOrGroupExecution) =
        RootExecutionGroup(members.toList())
}
