package io.liftgate.robotics.mono.gamepad

import io.liftgate.robotics.mono.gamepad.GamepadCommands.ButtonMapping

/**
 * @author GrowlyX
 * @since 10/14/2024
 */
class CommandBundle(
    val listeners: MutableMap<() -> Boolean, ButtonMapping>
)
{
    companion object
    {
        @JvmStatic
        fun from(gamepadCommands: GamepadCommands) = CommandBundle(listeners = gamepadCommands.listeners)
    }

    fun applyTo(commands: GamepadCommands)
    {
        commands.listeners.clear()
        commands.listeners += listeners
    }
}
