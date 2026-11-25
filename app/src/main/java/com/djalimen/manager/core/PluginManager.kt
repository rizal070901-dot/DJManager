package com.djalimen.manager.core

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

/** Local plugin catalogue. Importing a ZIP never executes its contents. */
object PluginManager {
    data class Plugin(
        val id: String,
        val name: String,
        val version: String,
        val description: String,
        val directory: File,
        val entryScript: File?,
        val webEntry: File?
    )

    private const val MAX_ENTRIES = 500
    private const val MAX_UNCOMPRESSED_BYTES = 32L * 1024L * 1024L

    private fun root(context: Context) = File(context.filesDir, "djalimen_plugins").apply { mkdirs() }

    fun list(context: Context): List<Plugin> = root(context).listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { dir ->
            runCatching {
                val manifestFile = File(dir, "plugin.json")
                val json = if (manifestFile.isFile) JSONObject(manifestFile.readText()) else JSONObject()
                val scriptName = json.optString("entry", "action.sh")
                val script = File(dir, scriptName).canonicalFile.takeIf {
                    it.path.startsWith(dir.canonicalPath + File.separator) && it.isFile && it.extension.equals("sh", true)
                }
                val webName = json.optString("web", "index.html")
                val web = File(dir, webName).canonicalFile.takeIf {
                    it.path.startsWith(dir.canonicalPath + File.separator) && it.isFile && it.extension.equals("html", true)
                }
                Plugin(
                    id = dir.name,
                    name = json.optString("name", dir.name),
                    version = json.optString("version", "1.0"),
                    description = json.optString("description", "Plugin lokal Djalimen"),
                    directory = dir,
                    entryScript = script,
                    webEntry = web
                )
            }.getOrNull()
        }?.sortedBy { it.name.lowercase() } ?: emptyList()

    @Synchronized
    fun install(context: Context, uri: Uri): Plugin {
        val base = root(context)
        val id = "plugin_${UUID.randomUUID().toString().take(8)}"
        val staging = File(base, ".$id.tmp").apply { mkdirs() }
        var entries = 0
        var totalBytes = 0L
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        entries++
                        require(entries <= MAX_ENTRIES) { "ZIP berisi terlalu banyak file (maksimum $MAX_ENTRIES)." }
                        val out = File(staging, entry.name).canonicalFile
                        require(out.path.startsWith(staging.canonicalPath + File.separator) || out == staging) { "ZIP tidak valid: path traversal terdeteksi." }
                        if (entry.isDirectory) out.mkdirs() else {
                            out.parentFile?.mkdirs()
                            FileOutputStream(out).use { fos ->
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    totalBytes += count
                                    require(totalBytes <= MAX_UNCOMPRESSED_BYTES) { "Ukuran ekstrak melewati batas 32 MB." }
                                    fos.write(buffer, 0, count)
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("Tidak dapat membaca ZIP.")

            val manifest = File(staging, "plugin.json")
            if (!manifest.exists()) {
                val legacy = listOf("action.sh", "service.sh").firstOrNull { File(staging, it).isFile }
                manifest.writeText(JSONObject().put("name", id).put("version", "1.0")
                    .put("description", "Plugin legacy; periksa skrip sebelum dijalankan.")
                    .put("entry", legacy ?: "action.sh").toString(2))
            } else {
                val json = JSONObject(manifest.readText())
                require(json.optString("name").isNotBlank()) { "plugin.json harus memiliki field name." }
                val entry = json.optString("entry", "action.sh")
                require(!entry.startsWith("/") && !entry.split('/').contains("..")) { "Entry script tidak valid." }
                require(entry.isEmpty() || entry.endsWith(".sh", true)) { "Entry hanya boleh berupa skrip .sh." }
            }
            val destination = File(base, id)
            check(staging.renameTo(destination)) { "Gagal menyimpan plugin." }
            return list(context).first { it.id == id }
        } catch (e: Exception) {
            staging.deleteRecursively()
            throw e
        }
    }

    fun remove(context: Context, plugin: Plugin): Boolean {
        val base = root(context).canonicalFile
        val target = plugin.directory.canonicalFile
        if (!target.path.startsWith(base.path + File.separator)) return false
        return target.deleteRecursively()
    }
}
