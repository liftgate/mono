<img src="mono.png" align="middle">
<p align="center">An intuitive Kotlin library that accelerates FTC software development.</p>
# Install
In `FtcRobotController` -> `build.gradle`:
```groovy
repositories {
  maven { url = "https://oss.liftgate.io/artifactory/opensource" }
}

dependencies {
  ...
  api("io.liftgate.robotics:ftc-monolib:7.0-R1")
  api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.2")
  api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
  api("net.mamoe.yamlkt:yamlkt:0.13.0")
}
```

# Includes:
- Autonomous structuring/state machines
- Driver control gamepad button mapping
- Robot hardware modularization with Subsystems
- Persistent file-based configurations (`Mono Konfig`)

### State machines:
State machines are used in autonomous implementations to plan out chains of tasks our robot takes *(execution groups)*. You need to know three key terms to understand our autonomous programs:
- `single`: A single task that is executed
- `simultaneous`: A group of tasks executed **simultaneously**.
- `consecutive`: A group of tasks executed **consecutively**.

You can start writing your execution group tasks in the body of a `Mono.buildExecutionGroup { /* here */ }` call. Here is a simple example:
```kotlin
Mono.buildExecutionGroup {
    single("Forward to tape") {
        move(-BlueLeft.MoveForwardToTape)
    }
}
```

And we can make it more complex...
```kotlin
Mono.buildExecutionGroup {
    simultaneous("move into tape") {
        single("Forward to tape") {
            move(-BlueLeft.MoveForwardToTape)
        }

        single("set extender to intake") {
            clawSubsystem.toggleExtender(
                ExtendableClaw.ExtenderState.Intake,
                force = true
            )
        }
    }
}
```

And even more complex...
```kotlin
    simultaneous("strafe into drop position") {
        consecutive("strafe") {
            single("strafe into position") {
                strafe(-BlueLeft.StrafeIntoPosition)
            }
          
            single("sync into heading") {
                turn(BlueLeft.TurnTowardsBackboard)
            }
        }
  
        single("heighten elevator") {
            elevatorSubsystem.configureElevatorManually(BlueLeft.ZElevatorDropExpectedHeight)
        }
    }
```

### Gamepad Mappings:
Gamepad mappings are used in our TeleOp code to make writing command executions triggered by the driver 1/2 gamepads easier. These gamepad mappings are created with a builder-style system. A simple example:
```kotlin
val gamepad = Mono.commands(gamepad1) // declare your GamepadCommands instance

// launch the airplane when you click the square button, and
// set it back to armed when you release the square button.
gp1Commands
    .where(ButtonType.PlayStationSquare)
    .triggers {
        paperPlaneLauncher.launch()
    }
    .andIsHeldUntilReleasedWhere {
        paperPlaneLauncher.reset()
    }
```

This builder system makes it very easy for us to build deposit presets in TeleOp:
```kotlin
//depositPresetReleaseOnElevatorHeight is hidden
gp2Commands
    .where(ButtonType.DPadLeft)
    .depositPresetReleaseOnElevatorHeight(-630)

gp2Commands
    .where(ButtonType.DPadUp)
    .depositPresetReleaseOnElevatorHeight(-850)

gp2Commands
    .where(ButtonType.DPadRight)
    .depositPresetReleaseOnElevatorHeight(-1130)
```

### Subsystems:
Our robot code is split up into multiple classes through Subsystems. Subsystems are independent components of the robot. An example of a subsystem is: `AirplaneLauncher`.

#### Lifecycle:
- Subsystem is registered in the subsystem registry (essentially a list of available subsystems we keep control over).
- When you press init on the opmode, subsystems are **initialized**.
    - Calls the `doInitialize()` function within the Subsystem implementation.
- When the opmode is stopped, the subsystems are **disposed**.
    - Calls the `dispose()` function within the Subsystem implementation.

Here is the real implementation of the `AirplaneLauncher`:
```kotlin
class AirplaneLauncher(private val opMode: LinearOpMode) : AbstractSubsystem()
{
    private val backingServo by lazy {
        opMode.hardware<Servo>("launcher")
    }

    /**
     * Puts the airplane launcher servo to the LAUNCHED position.
     */
    fun launch()
    {
        backingServo.position = ClawExpansionConstants.MAX_PLANE_POSITION
    }

    /**
     * Puts the airplane launcher servo to the ARMED position.
     */
    fun arm()
    {
        backingServo.position = ClawExpansionConstants.DEFAULT_PLANE_POSITION
    }

    /**
     * Initialize the servo when the subsystem is initalized, and arm the airplane launcher.
     */
    override fun doInitialize()
    {
        arm()
    }

    // do nothing since servos don't need to be reset on end
    override fun dispose()
    {

    }
}
```

### Konfig
Coming soon!
