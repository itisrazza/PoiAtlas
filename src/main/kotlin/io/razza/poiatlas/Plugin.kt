package io.razza.poiatlas

import io.razza.poiatlas.model.Poi
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class Plugin : JavaPlugin(), Listener {
    companion object {
        private var instanceField: Plugin? = null
        private var jdaField: JDA? = null
        private var channelIdField: String = ""

        val channel
            get() = jda.getChannelById(TextChannel::class.java, channelIdField)!!

        val instance: Plugin
            get() = instanceField!!

        val jda: JDA
            get() = jdaField!!
    }

    override fun onEnable() {
        instanceField = this
        Bukkit.getPluginManager().registerEvents(this, this)

        // create the local database
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        Poi.openDatabase(dataFolder)

        // connect to discord
        val discordToken = config.getString("discord.token")
        val channelId = config.getString("discord.channel")
        if (discordToken != null && channelId != null) {
            jdaField = JDABuilder.createDefault(discordToken).build().awaitReady()
            channelIdField = channelId
        }
        logger.log(Level.INFO, "Connected to Discord bot")

        setCommandExecutor("poi", Commands::poi)
        setCommandExecutor("poiall", Commands::poiAll)
        setCommandExecutor("poiadd", Commands::poiAdd)
        setCommandExecutor("poirm", Commands::poiRemove)
    }
}