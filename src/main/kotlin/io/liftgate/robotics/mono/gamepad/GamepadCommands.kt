package io.liftgate.robotics.mono.gamepad

import com.qualcomm.robotcore.hardware.Gamepad
import io.liftgate.robotics.mono.Mono
import io.liftgate.robotics.mono.states.State
import io.liftgate.robotics.mono.subsystem.AbstractSubsystem
import io.liftgate.robotics.mono.subsystem.Subsystem
import io.liftgate.robotics.mono.subsystem.terminable.composite.CompositeTerminable
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 9/4/2023
 */
class GamepadCommands internal constructor(private val gamepad: Gamepad) : Runnable, AbstractSubsystem()
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
        val dependencies: Set<State<*>>,
        val releaseTrigger: (() -> Unit)? = null,
        var delay: Long?,
        var lastTrigger: Long = 0L,
        var lock: Boolean = false,
    )

    private val listeners = mutableMapOf<() -> Boolean, ButtonMapping>()
    private var future: ScheduledFuture<*>? = null

    fun where(base: ButtonType) = ButtonMappingBuilder(usedButtons = mutableSetOf(base)) { isActive(base) }

    fun doBuild(base: ButtonDynamic, check: (Float) -> Boolean) =
        ButtonMappingBuilder(usedButtons = mutableSetOf()) { isActive(base, check) }

    fun whereDynamicGTE(base: ButtonDynamic, target: Float) = doBuild(base) { it >= target }
    fun whereDynamicGT(base: ButtonDynamic, target: Float) = doBuild(base) { it > target }
    fun whereDynamicLTE(base: ButtonDynamic, target: Float) = doBuild(base) { it <= target }
    fun whereDynamicLT(base: ButtonDynamic, target: Float) = doBuild(base) { it < target }
    fun whereDynamicEQ(base: ButtonDynamic, target: Float) = doBuild(base) { it == target }

    fun whereDynamicInRange(base: ButtonDynamic, range: IntRange) = doBuild(base) { it.toInt() in range }

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
                if (mapping.dependencies.isNotEmpty())
                {
                    if (!mapping.lock)
                    {
                        val dependenciesUsed = listeners.values
                            .filter { it.lock }
                            .flatMap { it.dependencies }
                            .toSet()

                        if (mapping.dependencies.intersect(dependenciesUsed).isNotEmpty())
                        {
                            Mono.logSink("Dependencies in use, skipping")
                            continue
                        }
                    }
                }

                // if this requires a lock (sirngle-use), don't continue until it's released
                if (mapping.behavior.requiresLock)
                {
                    if (mapping.lock)
                    {
                        Mono.logSink("Locking")
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
    private fun isActive(base: ButtonDynamic, comparison: (Float) -> Boolean) = comparison(base.position(gamepad))

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

        val dependencies = mutableSetOf<State<*>>()
        fun dependsOn(vararg states: State<*>) = apply {
            dependencies += states
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

            fun build(
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
                    dependencies = dependencies,
                    usedButtons = usedButtons.toSet()
                )
                built = true
            }
        }
    }

    private var manualUpdates = false

    fun doButtonUpdatesManually()
    {
        manualUpdates = true
    }

    override fun doInitialize()
    {
        if (manualUpdates)
        {
            return
        }

        check(future == null)
        future = Mono.COMMANDS.scheduleAtFixedRate(
            this, 0L, 50L, TimeUnit.MILLISECONDS
        )

        with {
            dispose()
        }
    }

    override fun start()
    {

    }

    override fun dispose() = with(this) {
        if (!manualUpdates)
        {
            future?.cancel(true)
            future = null
        }
    }

    override fun composeStageContext() = throw IllegalStateException("No completion stage in GamepadCommands")
    override fun isCompleted() = throw IllegalStateException("No completion state in GamepadCommands")
}
