package io.liftgate.robotics.mono.gamepad

import com.qualcomm.robotcore.hardware.Gamepad

/**
 * @author GrowlyX
 * @since 10/14/2024
 */
fun Gamepad.bundle(block: GamepadCommands.() -> Unit) = GamepadCommands(this).toBundle()
