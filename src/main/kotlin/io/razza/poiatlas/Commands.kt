package io.razza.poiatlas

import io.razza.poiatlas.model.Poi
import io.razza.poiatlas.model.Position
import io.razza.poiatlas.model.toPosition
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object Commands {
    fun poi(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            return poiAll(sender, command, label, args)
        }

        if (args.isNotEmpty()) {
            try {
                val poi = Poi.getByName(nameFromArgs(args));
                sender.sendMessage(poiString(poi, sender.location.toPosition()))
                return true
            } catch (e: UserException) {
                sender.sendMessage("Error: ${e.message}")
                return false
            }
        }

        val nearbyPoi = Poi.getAll()
            .sortedBy { it.position.distanceTo(sender.location.toPosition()) }
            .take(5)
        sender.sendMessage("Nearby points for interest")
        for (poi in nearbyPoi) {
            sender.sendMessage(poiString(poi, sender.location.toPosition()))
        }

        return true
    }

    fun poiAll(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        sender.sendMessage("All points of interest")
        val allPois = Poi.getAll()
        val allPoisSorted = if (sender is Player)
            allPois.sortedBy { it.position.distanceTo(sender.location.toPosition()) }
        else
            allPois.sortedBy { it.id }

        for (poi in allPoisSorted) {
            sender.sendMessage(poiString(poi, if (sender is Player) sender.location.toPosition() else null))
        }

        return true
    }

    fun poiAdd(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can add points of interest.")
            return false
        }

        if (args.isEmpty()) {
            sender.sendMessage("You need to give the location a name.")
            return false
        }

        val poi = Poi(name = nameFromArgs(args), position = sender.location.toPosition(), world = sender.world.name)
        try {
            poi.save()
        } catch (e: UserException) {
            sender.sendMessage(e.message!!)
            e.printStackTrace()
            return false
        }

        sender.sendMessage(
            """
            Saved location §b${poi.name}§r
            Location: ${poi.position}
            """.trimIndent()
        )

        return true
    }

    fun poiRemove(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("I need the name of the point of interest to remove.")
            return false
        }

        val poi = try {
            Poi.getByName(nameFromArgs(args))
        } catch (e: UserException) {
            sender.sendMessage(e.message!!)
            e.printStackTrace()
            return false
        }
        poi.delete()
        sender.sendMessage("§b${poi.name}§r was removed")

        return true
    }

    private fun nameFromArgs(args: Array<out String>) =
        args.joinToString(" ")

    private fun poiString(poi: Poi, position: Position? = null) = buildString {
        append("§b")
        append(poi.name)
        append("§r (")
        append(poi.world)
        append(" @ ")
        append(poi.position)
        append(")")

        if (position != null) {
            append(" - ")
            append(position.distanceTo(poi.position).toInt())
            append("m away")
        }
    }
}
