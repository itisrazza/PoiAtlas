package io.razza.poiatlas

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

fun JavaPlugin.setCommandExecutor(
    name: String,
    onCommand: (sender: CommandSender, command: Command, label: String, args: Array<out String>) -> Boolean
) {
    val command = this.getCommand(name)
        ?: throw NullPointerException("The command `$name' does not exist for this plugin.")
    command.setExecutor(onCommand)
}
