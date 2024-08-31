package io.liftgate.robotics.mono.states

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * @author GrowlyX
 * @since 8/31/2024
 */
abstract class StateHolder
{
    private val states = mutableSetOf<State<*>>()
    fun allPeriodic()
    {
        states.forEach {
            it.periodic()
        }
    }

    private inline fun <reified T : Any> state(
        noinline write: (T) -> Unit,
        noinline read: () -> T,
        noinline complete: (T, T) -> Boolean = { one, two -> one == two }
    ) = object : ReadOnlyProperty<Any, State<T>>
    {
        private val state = State(write, read, complete).apply { states += this }
        override fun getValue(thisRef: Any, property: KProperty<*>) = state
    }

}