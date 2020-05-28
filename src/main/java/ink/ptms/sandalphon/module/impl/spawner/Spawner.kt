package ink.ptms.sandalphon.module.impl.spawner

import ink.ptms.sandalphon.Sandalphon
import ink.ptms.sandalphon.module.impl.holographic.Hologram
import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockData
import ink.ptms.sandalphon.module.impl.scriptblock.data.BlockType
import ink.ptms.sandalphon.module.impl.spawner.data.SpawnerData
import ink.ptms.sandalphon.util.Utils
import io.izzel.taboolib.module.command.lite.CommandBuilder
import io.izzel.taboolib.module.db.local.LocalFile
import io.izzel.taboolib.module.inject.TFunction
import io.izzel.taboolib.module.inject.TInject
import io.izzel.taboolib.module.inject.TSchedule
import io.izzel.taboolib.module.packet.Packet
import io.izzel.taboolib.module.packet.TPacket
import io.izzel.taboolib.module.packet.TPacketHandler
import io.lumine.xikage.mythicmobs.MythicMobs
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler

object Spawner {

    @LocalFile("module/spawner.yml")
    lateinit var data: FileConfiguration
        private set

    val spawners = ArrayList<SpawnerData>()

    @TSchedule(period = 20)
    fun tick() {
        spawners.forEach { it.tick() }
    }

    @TSchedule
    fun import() {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            return
        }
        spawners.clear()
        data.getKeys(false).forEach {
            spawners.add(SpawnerData(Utils.toLocation(it.replace("__", ".")), MythicMobs.inst().mobManager.getMythicMob(data.getString("$it.mob"))).run {
                this.time.putAll(data.getConfigurationSection("$it.time")?.getValues(false)?.map { Utils.toLocation(it.key) to it.value as Long }?.toMap() ?: emptyMap())
                this.copy.addAll(data.getStringList("$it.link").map { link -> Utils.toLocation(link) })
                this.activationrange = data.getInt("$it.activationrange")
                this.leashrange = data.getInt("$it.leashrange")
                this.respawn = data.getInt("$it.respawn")
                this
            })
        }
    }

    @TFunction.Cancel
    fun export() {
        spawners.forEach { spawner ->
            val location = Utils.fromLocation(spawner.block).replace(".", "__")
            data.set("$location.time", spawner.time.map { Utils.fromLocation(it.key) to it.value }.toMap())
            data.set("$location.copy", spawner.copy.map { Utils.fromLocation(it) })
            data.set("$location.mob", spawner.mob.internalName)
            data.set("$location.activationrange", spawner.activationrange)
            data.set("$location.leashrange", spawner.leashrange)
            data.set("$location.respawn", spawner.respawn)
        }
    }

    @TFunction.Cancel
    fun cancel() {
        spawners.forEach { it.cancel() }
    }

    fun delete(location: String) {
        data.set(location.replace(".", "__"), null)
    }

    fun getSpawner(block: Block): SpawnerData? {
        return spawners.firstOrNull { it.isSpawner(block) }
    }
}