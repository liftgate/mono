package io.liftgate.robotics.mono.gamepad

import com.qualcomm.robotcore.hardware.Gamepad

/**
 * @author GrowlyX
 * @since 11/17/2023
 */
enum class ButtonDynamic(val position: (Gamepad) -> Float)
{
    TriggerLeft(Gamepad::left_trigger),
    TriggerRight(Gamepad::right_trigger),

    StickRightX(Gamepad::right_stick_x),
    StickRightY(Gamepad::right_stick_y),

    StickLeftX(Gamepad::right_stick_x),
    StickLeftY(Gamepad::right_stick_y)
}
