/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.block.BlockLadder
import net.minecraft.block.BlockVine
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object FastClimb : Module("FastClimb", Category.MOVEMENT) {

    val mode by choices(
        "Mode",
        arrayOf("Vanilla", "Delay", "Clip", "AAC3.0.0", "AAC3.0.5", "SAAC3.1.2", "AAC3.1.2"), "Vanilla"
    )
    private val speed by float("Speed", 1F, 0.01F..5F) { mode == "Vanilla" }

    // Delay mode | Separated Vanilla & Delay speed value
    private val climbSpeed by float("ClimbSpeed", 1F, 0.01F..5F) { mode == "Delay" }
    private val tickDelay by int("TickDelay", 10, 1..20) { mode == "Delay" }


    private val climbDelay = tickDelay
    private var climbCount = 0

    private fun playerClimb() {
        mc.thePlayer.motionY = 0.0
        mc.thePlayer.isInWeb = true
        mc.thePlayer.onGround = true

        mc.thePlayer.isInWeb = false
    }

    val onMove = handler<MoveEvent> { event ->
        val mode = mode

        val thePlayer = mc.thePlayer ?: return@handler

        when {
            mode == "Vanilla" && thePlayer.isCollidedHorizontally && thePlayer.isOnLadder -> {
                event.y = speed.toDouble()
                thePlayer.motionY = 0.0
            }

            mode == "Delay" && thePlayer.isCollidedHorizontally && thePlayer.isOnLadder -> {

                if (climbCount >= climbDelay) {

                    event.y = climbSpeed.toDouble()
                    playerClimb()

                    val currentPos =
                        C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true)

                    sendPacket(currentPos)

                    climbCount = 0

                } else {
                    thePlayer.posY = thePlayer.prevPosY

                    playerClimb()
                    climbCount += 1

                }
            }


            mode == "AAC3.0.0" && thePlayer.isCollidedHorizontally -> {
                var x = 0.0
                var z = 0.0

                when (thePlayer.horizontalFacing) {
                    EnumFacing.NORTH -> z = -0.99
                    EnumFacing.EAST -> x = 0.99
                    EnumFacing.SOUTH -> z = 0.99
                    EnumFacing.WEST -> x = -0.99
                    else -> {}
                }

                val block = BlockPos(thePlayer.posX + x, thePlayer.posY, thePlayer.posZ + z).block

                if (block is BlockLadder || block is BlockVine) {
                    event.y = 0.5
                    thePlayer.motionY = 0.0
                }
            }

            mode == "AAC3.0.5" && mc.gameSettings.keyBindForward.isKeyDown &&
                    collideBlockIntersects(thePlayer.entityBoundingBox) {
                        it is BlockLadder || it is BlockVine
                    } -> {
                event.x = 0.0
                event.y = 0.5
                event.z = 0.0

                thePlayer.motionX = 0.0
                thePlayer.motionY = 0.0
                thePlayer.motionZ = 0.0
            }

            mode == "SAAC3.1.2" && thePlayer.isCollidedHorizontally &&
                    thePlayer.isOnLadder -> {
                event.y = 0.1649
                thePlayer.motionY = 0.0
            }

            mode == "AAC3.1.2" && thePlayer.isCollidedHorizontally &&
                    thePlayer.isOnLadder -> {
                event.y = 0.1699
                thePlayer.motionY = 0.0
            }

            mode == "Clip" && thePlayer.isOnLadder && mc.gameSettings.keyBindForward.isKeyDown -> {
                for (i in thePlayer.posY.toInt()..thePlayer.posY.toInt() + 8) {
                    val block = BlockPos(thePlayer.posX, i.toDouble(), thePlayer.posZ).block

                    if (block !is BlockLadder) {
                        var x = 0.0
                        var z = 0.0

                        when (thePlayer.horizontalFacing) {
                            EnumFacing.NORTH -> z = -1.0
                            EnumFacing.EAST -> x = 1.0
                            EnumFacing.SOUTH -> z = 1.0
                            EnumFacing.WEST -> x = -1.0
                            else -> {}
                        }

                        thePlayer.setPosition(thePlayer.posX + x, i.toDouble(), thePlayer.posZ + z)
                        break
                    } else {
                        thePlayer.setPosition(thePlayer.posX, i.toDouble(), thePlayer.posZ)
                    }
                }
            }
        }
    }

    val onBlockBB = handler<BlockBBEvent> { event ->
        if (mc.thePlayer != null && (event.block is BlockLadder || event.block is BlockVine) &&
            mode == "AAC3.0.5" && mc.thePlayer.isOnLadder
        )
            event.boundingBox = null
    }

    override val tag
        get() = mode
}