package io.liftgate.robotics.mono

import com.qualcomm.robotcore.hardware.Gamepad
import io.liftgate.robotics.mono.gamepad.GamepadCommands

/**
 * @author GrowlyX
 * @since 9/5/2023
 */
object Mono
{
    fun commands(gamepad: Gamepad) = GamepadCommands(gamepad)
}
