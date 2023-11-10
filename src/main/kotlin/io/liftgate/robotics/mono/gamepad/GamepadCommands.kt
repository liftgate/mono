package io.liftgate.robotics.mono.gamepad

import com.qualcomm.robotcore.hardware.Gamepad
import io.liftgate.robotics.mono.Mono
import io.liftgate.robotics.mono.subsystem.Subsystem
import io.liftgate.robotics.mono.subsystem.terminable.composite.CompositeTerminable
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 9/4/2023
 */
class GamepadCommands internal constructor(private val gamepad: Gamepad) : Runnable, Subsystem, CompositeTerminable by CompositeTerminable.create()
{
    enum class ButtonBehavior(val requiresLock: Boolean = false)
    {
        Continuous,
        Single(requiresLock = true)
    }

    data class ButtonMapping(
        val handler: () -> Unit,
        val behavior: ButtonBehavior,
        val usedButtons: Set<ButtonType>,
        val releaseTrigger: (() -> Unit)? = null,
        var delay: Long?,
        var lastTrigger: Long = 0L,
        var lock: Boolean = false,
    )

    private val listeners = mutableMapOf<() -> Boolean, ButtonMapping>()
    private var future: ScheduledFuture<*>? = null

    fun where(base: ButtonType) = ButtonMappingBuilder(usedButtons = mutableSetOf(base)) { isActive(base) }

    override fun run()
    {
        val buttonsTriggered = mutableSetOf<ButtonType>()
        for ((expr, mapping) in listeners.entries.sortedByDescending { it.value.usedButtons.size })
        {
            // a higher priority button (B + Y rather than B) was triggered.
            if (mapping.usedButtons.intersect(buttonsTriggered).isNotEmpty())
            {
                continue
            }

            // if the expression is true, trigger the handler.
            if (expr())
            {
                // if this requires a lock (single-use), don't continue until it's released
                if (mapping.behavior.requiresLock)
                {
                    if (mapping.lock)
                    {
                        continue
                    }
                }

                // we lock just in-case for release triggers to work properly
                mapping.lock = true

                if (mapping.delay != null)
                {
                    // Ensure we don't exceed the delay
                    if (mapping.lastTrigger + mapping.delay!! > System.currentTimeMillis())
                    {
                        continue
                    }

                    mapping.lastTrigger = System.currentTimeMillis()
                }

                runCatching {
                    buttonsTriggered += mapping.usedButtons
                    mapping.handler()
                }.onFailure {
                    it.printStackTrace()
                }
                continue
            }

            // If previously locked, and a release trigger is set, release the lock.
            val releaseTrigger = mapping.releaseTrigger
            if (releaseTrigger != null && mapping.lock)
            {
                releaseTrigger()
            }

            mapping.lock = false
            mapping.lastTrigger = 0L
        }
    }

    private fun isActive(base: ButtonType) = base.gamepadMapping(gamepad)

    inner class ButtonMappingBuilder(
        private val usedButtons: MutableSet<ButtonType> = mutableSetOf(),
        private var expression: () -> Boolean = { true }
    )
    {
        fun and(buttonType: ButtonType) = apply {
            val prevExp = expression
            usedButtons += buttonType
            expression = { prevExp() && isActive(buttonType) }
        }

        fun or(customizer: ButtonMappingBuilder.() -> ButtonMappingBuilder) = apply {
            val prevExp = expression
            val customized = customizer(ButtonMappingBuilder())
            val customizedExpr = customized.expression

            usedButtons += customized.usedButtons
            expression = { prevExp() || customizedExpr() }
        }

        fun onlyWhen(lambda: () -> Boolean) = apply {
            val prevExp = expression
            expression = { prevExp() && lambda() }
        }

        fun onlyWhenNot(lambda: () -> Boolean) = apply {
            val prevExp = expression
            expression = { prevExp() && !lambda() }
        }

        @JvmOverloads
        fun triggers(delay: Long? = null, executor: () -> Unit): InternalButtonMappingBuilderWithExecutor
        {
            check(delay == null || delay >= 50L) {
                "Delay cannot be less than 50ms as that is the tick speed"
            }

            return InternalButtonMappingBuilderWithExecutor(executor, delay)
        }

        inner class InternalButtonMappingBuilderWithExecutor(
            private val executor: () -> Unit,
            private val delay: Long?,
            private var built: Boolean = false
        )
        {
            fun whenPressedOnce()
            {
                check(delay == null) {
                    "Delay is not applicable to single-press buttons"
                }

                check(!built) {
                    "Button already mapped"
                }
                build(ButtonBehavior.Single)
            }

            fun repeatedlyWhilePressed()
            {
                check(!built) {
                    "Button already mapped"
                }
                build(ButtonBehavior.Continuous, delay = delay ?: 50L)
            }

            fun repeatedlyWhilePressedUntilReleasedWhere(lockRelease: () -> Unit)
            {
                check(!built) {
                    "Button already mapped"
                }
                build(ButtonBehavior.Continuous, lockRelease, delay ?: 50L)
            }

            fun andIsHeldUntilReleasedWhere(lockRelease: () -> Unit)
            {
                check(delay == null) {
                    "Delay is not applicable to held buttons"
                }
                check(!built) {
                    "Button already mapped"
                }

                // behavior is technically single
                build(ButtonBehavior.Single, lockRelease)
            }

            private fun build(
                behavior: ButtonBehavior,
                onRelease: (() -> Unit)? = null,
                delay: Long? = null
            ) = also {
                // return unit to prevent chaining of commands which is HIDEOUS
                listeners[expression] = ButtonMapping(
                    handler = executor,
                    behavior = behavior,
                    releaseTrigger = onRelease,
                    delay = delay,
                    usedButtons = usedButtons.toSet()
                )
                built = true
            }
        }
    }

    override fun initialize()
    {
        check(future == null)
        future = Mono.COMMANDS.scheduleAtFixedRate(
            this, 0L, 50L, TimeUnit.MILLISECONDS
        )

        with {
            dispose()
        }
    }

    override fun dispose() = with(this) {
        checkNotNull(future)
        future!!.cancel(true)
        future = null
    }

    override fun composeStageContext() = throw IllegalStateException("No completion stage in GamepadCommands")
    override fun isCompleted() = throw IllegalStateException("No completion state in GamepadCommands")
}
