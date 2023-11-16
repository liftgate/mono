# mono
Boilerplate systems and frameworks for FTC code.

## Features:
- Commands (gamepad control commands)
- "Subsystems/CommandBases" -> Task Execution Groups (for FTC autonomous)

## Commands:
Control gamepad buttons in a builder fashion using Mono Commands.

Example:
```kotlin
val commands = Mono.commands(gamepad1)
commands.where(ButtonType.ButtonA)
    .triggers {
        println("hey")
    }
    .whenPressedOnce()
```

_More complex example:_
```kotlin
commands
    .where(ButtonType.ButtonA)
    .onlyWhenNot { bundleExecutionInProgress }
    .triggers {
        bundleExecutionInProgress = true
        extendableClaw.toggleExtender(
            ExtendableClaw.ExtenderState.Intake
        )

        extendableClaw.updateClawState(
            ExtendableClaw.ClawStateUpdate.Both,
            ExtendableClaw.ClawState.Open
        )
    }
    .andIsHeldUntilReleasedWhere {
        extendableClaw.updateClawState(
            ExtendableClaw.ClawStateUpdate.Both,
            ExtendableClaw.ClawState.Closed
        )

        scheduleAsyncExecution(450L) {
            extendableClaw.toggleExtender(
                ExtendableClaw.ExtenderState.Deposit
            )

            bundleExecutionInProgress = false
        }
    }
```

## Execution Groups:
Run controlled chains of tasks using Mono Execution Groups!

```kotlin
val group = Mono.buildExecutionGroup {
    single("detect team element") {
        // TODO
        Thread.sleep(1000L)
        // do some detection logic and find the tape side
        put("tape", TapeSide.Left)
    }
    // executed simultaneously
    parallel(
        "move and lower elevator"
    ) {
        single("move towards tape") {
            val tapeSide = require<TapeSide>("tape")
            Thread.sleep(1000L)
            // TODO
        }
        single("lower elevator") {
            Thread.sleep(1000L)
            // TODO
        }
    }
}
```
