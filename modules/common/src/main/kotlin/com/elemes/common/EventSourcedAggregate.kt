package com.elemes.common

/**
 * Generic base for the aggregates in the compliance-critical tier (Ch.11 §5:
 * Assignment & Enrollment, Assessment & Question Bank, Certification &
 * Compliance) — state only ever changes by raising and applying an event,
 * so replaying history always reproduces the exact current state.
 */
abstract class EventSourcedAggregate<E> {

    var version: Long = 0
        private set

    private val _uncommittedEvents = mutableListOf<E>()
    val uncommittedEvents: List<E> get() = _uncommittedEvents.toList()

    protected fun raise(event: E) {
        apply(event)
        _uncommittedEvents += event
        version++
    }

    fun loadFromHistory(events: List<E>) {
        events.forEach {
            apply(it)
            version++
        }
    }

    fun markCommitted() {
        _uncommittedEvents.clear()
    }

    protected abstract fun apply(event: E)
}
