package com.alexmojaki.quiggles

interface Clock {
    fun tick()
    fun now(): Long
}

class SystemClock : Clock {
    override fun tick() {
    }

    override fun now(): Long = System.currentTimeMillis()

}

class ControlledClock(val tickLength: Int) : Clock {
    var now = clock.now()
    override fun tick() {
        now += tickLength
    }

    override fun now(): Long = now

}

var clock: Clock = SystemClock()
