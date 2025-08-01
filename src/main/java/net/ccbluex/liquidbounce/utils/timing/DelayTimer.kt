/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.utils.timing

import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.CLICK_TIMER
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomDelay

open class DelayTimer(
    private val minDelayValue: Int, private val maxDelayValue: Int = minDelayValue,
    private val baseTimer: MSTimer = CLICK_TIMER
) {
    private var delay = 0

    open fun hasTimePassed() = baseTimer.hasTimePassed(delay)

    fun resetDelay() {
        delay = randomDelay(minDelayValue, maxDelayValue)
    }

    fun resetTimer() = baseTimer.reset()

    fun reset() {
        resetTimer()
        resetDelay()
    }
}

open class TickDelayTimer(
    private val minDelayValue: Int, private val maxDelayValue: Int = minDelayValue,
    private val baseTimer: TickTimer = TickTimer()
) {
    private var ticks = 0

    open fun hasTimePassed() = baseTimer.hasTimePassed(ticks)

    fun resetTicks() {
        ticks = randomDelay(minDelayValue, maxDelayValue)
    }

    fun resetTimer() = baseTimer.reset()

    fun update() = baseTimer.update()

    fun reset() {
        resetTimer()
        resetTicks()
    }
}