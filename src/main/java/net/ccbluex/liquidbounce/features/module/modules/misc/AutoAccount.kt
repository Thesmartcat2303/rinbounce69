/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import me.liuli.elixir.account.CrackedAccount
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.EventManager.call
import net.ccbluex.liquidbounce.event.async.launchSequence
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.file.FileManager.accountsConfig
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.client.ServerUtils
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.randomAccount
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S40PacketDisconnect
import net.minecraft.network.play.server.S45PacketTitle
import net.minecraft.util.ChatComponentText
import net.minecraft.util.Session

object AutoAccount :
    Module("AutoAccount", Category.MISC, subjective = true, gameDetecting = false) {

    private val register by boolean("AutoRegister", true)
    private val login by boolean("AutoLogin", true)

    private const val DEFAULT_PASSWORD = "ngovanhao"

    private val passwordValue = text("Password", DEFAULT_PASSWORD) {
        register || login
    }.onChange { old, new ->
        when {
            new.any { it.isWhitespace() } -> {
                chat("§7[§a§lAutoAccount§7] §cPassword cannot contain a space!")
                old
            }

            new.lowercase() == "reset" -> {
                chat("§7[§a§lAutoAccount§7] §3Password reset to its default value.")
                DEFAULT_PASSWORD
            }

            new.length < 4 -> {
                chat("§7[§a§lAutoAccount§7] §cPassword must be longer than 4 characters!")
                old
            }

            else -> new
        }
    }.subjective()

    private val password by passwordValue

    private val sendDelay by intRange("SendDelay", 150..300, 0..500) { passwordValue.isSupported() }

    private val autoSession by boolean("AutoSession", false)
    private val startupValue = boolean("RandomAccountOnStart", false) { autoSession }
    private val relogInvalidValue = boolean("RelogWhenPasswordInvalid", true) { autoSession }
    private val relogKickedValue = boolean("RelogWhenKicked", false) { autoSession }

    private val reconnectDelayValue = int("ReconnectDelay", 1000, 0..2500)
    { relogInvalidValue.isActive() || relogKickedValue.isActive() }
    private val reconnectDelay by reconnectDelayValue

    private val accountModeValue = choices("AccountMode", arrayOf("RandomName", "RandomAlt"), "RandomName") {
        reconnectDelayValue.isSupported() || startupValue.isActive()
    }.onChange { old, new ->
        if (new == "RandomAlt" && accountsConfig.accounts.filterIsInstance<CrackedAccount>().size <= 1) {
            chat("§7[§a§lAutoAccount§7] §cAdd more cracked accounts in AltManager to use RandomAlt option!")
            old
        } else {
            new
        }
    }
    private val accountMode by accountModeValue

    private val saveValue = boolean("SaveToAlts", false) {
        accountModeValue.isSupported() && accountMode != "RandomAlt"
    }

    private var status = Status.WAITING

    private fun relog(info: String = "") {
        if (mc.currentServerData != null && mc.theWorld != null)
            mc.netHandler.networkManager.closeChannel(
                ChatComponentText("$info\n\nReconnecting with a random account in ${reconnectDelay}ms")
            )


        changeAccount()

        launchSequence(Dispatchers.Main) {
            delay(sendDelay.random().toLong())

            ServerUtils.connectToLastServer()
        }
    }

    private fun respond(msg: String) = when {
        register && "/reg" in msg -> {
            addNotification(Notification.informative(this, "Trying to register."))
            launchSequence(Dispatchers.IO) {
                delay(sendDelay.random().toLong())
                mc.thePlayer.sendChatMessage("/register $password $password")
            }
            true
        }

        login && "/log" in msg -> {
            addNotification(Notification.informative(this, "Trying to log in."))
            launchSequence(Dispatchers.IO) {
                delay(sendDelay.random().toLong())
                mc.thePlayer.sendChatMessage("/login $password")
            }
            true
        }

        else -> false
    }

    val onPacket = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is S02PacketChat, is S45PacketTitle -> {
                if (!passwordValue.isSupported() || status == Status.STOPPED) return@handler

                val msg = when (packet) {
                    is S02PacketChat -> packet.chatComponent?.unformattedText?.lowercase()
                    is S45PacketTitle -> packet.message?.unformattedText?.lowercase()
                    else -> return@handler
                } ?: return@handler

                if (status == Status.WAITING) {
                    if (!respond(msg))
                        return@handler

                    event.cancelEvent()
                    status = Status.SENT_COMMAND
                } else {
                    when {
                        "success" in msg || "logged" in msg || "registered" in msg -> {
                            success()
                            event.cancelEvent()
                        }
                        "incorrect" in msg || "wrong" in msg || "spatne" in msg -> fail()
                        "unknown" in msg || "command" in msg || "allow" in msg || "already" in msg -> {
                            status = Status.STOPPED
                            event.cancelEvent()
                        }
                    }
                }
            }

            is S40PacketDisconnect -> {
                if (relogKickedValue.isActive() && status != Status.SENT_COMMAND) {
                    val reason = packet.reason.unformattedText
                    if ("ban" in reason) return@handler

                    relog(packet.reason.unformattedText)
                }
            }
        }

    }

    val onWorld = handler<WorldEvent> { event ->
        if (!passwordValue.isSupported()) return@handler

        if (mc.theWorld == null) {
            status = Status.WAITING
            return@handler
        }

        if (status == Status.SENT_COMMAND) {
            if (event.worldClient != null && mc.theWorld != event.worldClient) success()
            else fail()
        }
    }

    val onStartup = handler<StartupEvent> {
        if (startupValue.isActive()) changeAccount()
    }

    private fun success() {
        if (status == Status.SENT_COMMAND) {
            addNotification(Notification.informative(this, "Logged in as ${mc.session.username}"))

            status = Status.STOPPED
        }
    }

    private fun fail() {
        if (status == Status.SENT_COMMAND) {
            addNotification(Notification.error(this, "Failed to log in as ${mc.session.username}"))

            status = Status.STOPPED

            if (relogInvalidValue.isActive()) relog()
        }
    }

    private fun changeAccount() {
        if (accountMode == "RandomAlt") {
            val account = accountsConfig.accounts.filter { it is CrackedAccount && it.name != mc.session.username }
                .randomOrNull() ?: return
            mc.session = Session(
                account.session.username, account.session.uuid,
                account.session.token, account.session.type
            )
            call(SessionUpdateEvent)
            return
        }

        val account = randomAccount()

        if (saveValue.isActive() && !accountsConfig.accountExists(account)) {
            accountsConfig.addAccount(account)
            accountsConfig.saveConfig()

            addNotification(Notification.informative(this, "Saved alt ${account.name}"))
        }
    }

    private enum class Status {
        WAITING, SENT_COMMAND, STOPPED
    }
}
