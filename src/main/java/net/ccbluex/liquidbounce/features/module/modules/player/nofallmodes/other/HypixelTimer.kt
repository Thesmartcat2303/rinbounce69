/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.movement.FallingPlayer
import net.ccbluex.liquidbounce.utils.timing.TickedActions.nextTick
import net.minecraft.network.play.client.C03PacketPlayer

/*
* Working on Watchdog
* Tested on: mc.hypixel.net
* Credit: @localpthebest / HypixelPacket
*/
object HypixelTimer : NoFallMode("HypixelTimer") {

    override fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
        val packet = event.packet

        val fallingPlayer = FallingPlayer()

        if (packet is C03PacketPlayer) {
            if (fallingPlayer.findCollision(500) != null && player.fallDistance - player.motionY >= 3.3) {
                mc.timer.timerSpeed = 0.5f

                packet.onGround = true
                player.fallDistance = 0f

                NoFall.nextTick {
                    mc.timer.timerSpeed = 1f
                }
            }
        }
    }
}