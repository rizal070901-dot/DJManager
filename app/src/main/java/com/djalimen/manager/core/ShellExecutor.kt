package com.djalimen.manager.core

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Executes commands only through an explicitly granted Shizuku session or an installed root manager. */
object ShellExecutor {
    data class ServerStatus(val isRunning: Boolean, val uid: Int, val mode: String)
    private data class CommandResult(val stdout: String, val stderr: String, val exitCode: Int, val timedOut: Boolean = false) {
        fun combined(): String = buildString {
            if (stdout.isNotBlank()) append(stdout.trimEnd())
            if (stderr.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append(stderr.trimEnd())
            }
            if (isEmpty()) append(if (timedOut) "Perintah melewati batas waktu." else "Selesai (exit code $exitCode), tidak ada keluaran.")
            if (timedOut) append("\n[Proses dihentikan setelah batas waktu]")
            else if (exitCode != 0) append("\n[exit code $exitCode]")
        }
    }

    fun requestShizukuPermission(): String = try {
        if (!Shizuku.pingBinder()) {
            "Shizuku belum aktif. Buka Shizuku, mulai layanan, lalu coba lagi."
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            "Izin Shizuku sudah diberikan."
        } else {
            Shizuku.requestPermission(REQUEST_CODE)
            "Permintaan izin dikirim. Setujui dialog dari Shizuku."
        }
    } catch (e: Exception) {
        "Tidak dapat menghubungi Shizuku: ${e.message ?: "kesalahan tidak diketahui"}"
    }

    suspend fun checkStatus(): ServerStatus = withContext(Dispatchers.IO) {
        val shell = runShizuku("id -u")
        val shellUid = shell?.takeIf { !it.timedOut && it.exitCode == 0 }
            ?.stdout?.trim()?.toIntOrNull()
        if (shellUid != null) return@withContext statusFor(shellUid, "Shizuku / Shell")

        val root = runRoot("id -u")
        val rootUid = root?.takeIf { !it.timedOut && it.exitCode == 0 }
            ?.stdout?.trim()?.toIntOrNull()
        if (rootUid != null && rootUid == 0) return@withContext statusFor(0, "Root")

        ServerStatus(false, -1, "Tidak terhubung — aktifkan Shizuku atau akses Root")
    }

    suspend fun runCommand(cmd: String): String = withContext(Dispatchers.IO) {
        if (cmd.isBlank()) return@withContext "Perintah kosong."
        val shell = runShizuku(cmd)
        if (shell != null) return@withContext shell.combined()
        val root = runRoot(cmd)
        if (root != null) return@withContext root.combined()
        "Akses belum tersedia. Aktifkan layanan Shizuku dan berikan izin, atau gunakan perangkat dengan Root. Brevent tidak menyediakan API umum agar aplikasi lain otomatis memakai sesi shell-nya."
    }

    private fun statusFor(uid: Int, source: String): ServerStatus {
        val mode = when (uid) {
            0 -> "Root (UID 0)"
            2000 -> "$source (UID 2000)"
            else -> "$source (UID $uid)"
        }
        return ServerStatus(true, uid, mode)
    }

    private fun runShizuku(command: String): CommandResult? {
        return try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) null
            else readProcess(Shizuku.newProcess(arrayOf("sh", "-c", command), null, null))
        } catch (_: Exception) {
            null
        }
    }

    private fun runRoot(command: String): CommandResult? {
        return try {
            readProcess(Runtime.getRuntime().exec(arrayOf("su", "-c", command)))
        } catch (_: Exception) {
            null
        }
    }

    /** Drain stdout and stderr concurrently so a full pipe cannot deadlock QuickShell. */
    private fun readProcess(process: Process): CommandResult {
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val outThread = thread(name = "dj-shell-stdout") {
            runCatching { process.inputStream.bufferedReader().use { stdout.append(it.readText()) } }
        }
        val errThread = thread(name = "dj-shell-stderr") {
            runCatching { process.errorStream.bufferedReader().use { stderr.append(it.readText()) } }
        }
        val completed = try { process.waitFor(30, TimeUnit.SECONDS) } catch (_: InterruptedException) {
            Thread.currentThread().interrupt(); false
        }
        if (!completed) process.destroyForcibly()
        runCatching { outThread.join(1500) }
        runCatching { errThread.join(1500) }
        return CommandResult(stdout.toString(), stderr.toString(), if (completed) process.exitValue() else -1, !completed)
    }

    suspend fun startWithRoot(context: Context): Boolean = withContext(Dispatchers.IO) {
        val result = runRoot("id -u")
        result != null && !result.timedOut && result.exitCode == 0 && result.stdout.trim() == "0"
    }

    fun getAdbActivationCommand(context: Context): String =
        "Aktifkan layanan Shizuku melalui Wireless Debugging, lalu buka Djalimen Manager dan pilih Hubungkan Shizuku."

    private const val REQUEST_CODE = 7312
}
