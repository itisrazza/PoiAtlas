package io.razza.poiatlas.model

import io.razza.poiatlas.Plugin
import io.razza.poiatlas.UserException
import net.dv8tion.jda.api.EmbedBuilder
import okhttp3.internal.wait
import org.bukkit.Location
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.logging.Level
import kotlin.math.abs
import kotlin.math.sqrt

data class Position(
    val x: Int,
    val y: Int,
    val z: Int
) {
    fun distanceTo(position: Position): Double {
        val dx = abs(x - position.x)
        val dy = abs(y - position.y)
        val dz = abs(z - position.z)

        return sqrt((dx * dx + dy * dy + dz * dz).toDouble())
    }

    override fun toString() = "$x, $y, $z"
}

fun Location.toPosition() = Position(x.toInt(), y.toInt(), z.toInt())

class Poi(id: Int? = null, name: String, world: String, position: Position, discordMessageId: String? = null) {
    var id: Int? = id
        private set

    val name: String = name
    val world: String = world
    val position: Position = position

    var discordMessageId: String? = discordMessageId
        private set

    fun save() {
        // send the message to Discord and get its message ID
        discordMessageId = Plugin.channel!!.sendMessageEmbeds(
            EmbedBuilder()
                .setTitle(name)
                .setDescription(position.toString())
                .addField("World", world, true)
                .addField("Location", position.toString(), true)
                .build()
        ).complete().id

        // save the POI in the database
        try {
            id =
                connection!!.prepareStatement("INSERT INTO poi (name, world, discord_message_id, x, y, z) VALUES (?, ?, ?, ?, ?, ?)")
                    .use {
                        it.setString(1, name)
                        it.setString(2, world)
                        it.setString(3, discordMessageId)
                        it.setInt(4, position.x)
                        it.setInt(5, position.y)
                        it.setInt(6, position.z)

                        it.executeUpdate()
                        val result = it.generatedKeys
                        if (result.next()) return@use result.getInt(1)
                        TODO("error condition not implemented: it didn't generate new keys, why")
                    }
        } catch (e: SQLiteException) {
            if (e.resultCode == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE) {
                throw UserException("A point of interest with this name already exists.", e)
            }

            throw RuntimeException(e)
        }
    }

    fun delete() {
        val id = this.id ?: return
        // this object is not persisted, so we can fail quietly here

        // remove the related discord message
        try {
            val discordMessageId = this.discordMessageId
            if (discordMessageId != null) {
                Plugin.channel.retrieveMessageById(discordMessageId).complete().delete().complete()
            }
        } catch (e: Exception) {
            // we should still delete the things even though the message might not be deleted
            e.printStackTrace()
        }

        connection!!.prepareStatement("DELETE FROM poi WHERE id = ?").apply {
            setInt(1, id)
        }.use {
            it.executeUpdate()
        }
    }

    override fun hashCode() = Objects.hash(name, position)

    companion object {
        fun getAll(): Collection<Poi> {
            val allPois = HashSet<Poi>()

            val result = connection!!.prepareStatement(
                """
                SELECT * FROM poi
                """.trimIndent()
            ).use {
                val result = it.executeQuery()
                result.use {
                    while (result.next()) {
                        allPois.add(
                            Poi(
                                id = result.getInt("id"),
                                name = result.getString("name")!!,
                                world = result.getString("world")!!,
                                position = Position(
                                    result.getInt("x"),
                                    result.getInt("y"),
                                    result.getInt("z")
                                ),
                                discordMessageId = result.getString("discord_message_id")
                            )
                        )
                    }
                }
            }

            return allPois
        }

        fun getByName(name: String): Poi {
            connection!!.prepareStatement("SELECT * FROM poi WHERE name = ?").apply {
                setString(1, name)
            }.use {
                it.executeQuery().use { result ->
                    if (!result.next()) {
                        throw UserException("Point of interest with the name \"$name\" was not found.")
                    }
                    return Poi(
                        id = result.getInt("id"),
                        name = result.getString("name")!!,
                        world = result.getString("world")!!,
                        position = Position(
                            result.getInt("x"),
                            result.getInt("y"),
                            result.getInt("z")
                        ),
                        discordMessageId = result.getString("discord_message_id")
                    )
                }
            }
        }

        private var connection: Connection? = null

        internal fun openDatabase(dataFolder: File) {
            val path = dataFolder.resolve("poidata.sqlite")
            val connection = DriverManager.getConnection("jdbc:sqlite:${path.absolutePath}")
            Plugin.instance.logger.log(Level.INFO, "Opened database: $path")

            if (!areTablesCreated(connection)) {
                createTables(connection)
                Plugin.instance.logger.log(Level.INFO, "Created new database tables")
            }

            this.connection = connection
        }

        private fun areTablesCreated(connection: Connection): Boolean {
            val expectedTables = setOf("poi")
            val foundTables = HashSet<String>()

            val result = connection.prepareStatement("SELECT * FROM sqlite_schema;")
                .executeQuery()
            while (result.next()) {
                foundTables.add(result.getString("name"))
            }
            result.close()

            return foundTables.containsAll(expectedTables)
        }

        private fun createTables(connection: Connection) {
            connection.prepareStatement(
                """
                CREATE TABLE "poi" (
                    "id"                 INTEGER NOT NULL UNIQUE,
                    "name"	             TEXT    NOT NULL UNIQUE,
                    "world"              TEXT    NOT NULL,
                    "discord_message_id" TEXT,
                    "x"	                 INTEGER NOT NULL,
                    "y"	                 INTEGER NOT NULL,
                    "z"	                 INTEGER NOT NULL,
                    PRIMARY KEY("id" AUTOINCREMENT)
                );
                """
            ).execute()
        }
    }
}
