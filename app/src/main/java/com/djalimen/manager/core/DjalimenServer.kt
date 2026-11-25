package com.djalimen.manager.core

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.Process
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.concurrent.Executors

object DjalimenServer {
    const val SOCKET_NAME = "djalimen_server_sock"
    private val executor = Executors.newCachedThreadPool()

    @JvmStatic
    fun main(args: Array<String>) {
        println("[DjalimenServer] Menjalankan daemon pada UID: ${Process.myUid()}")
        try {
            val serverSocket = LocalServerSocket(SOCKET_NAME)
            while (true) {
                val client: LocalSocket = serverSocket.accept()
                executor.execute { handleClient(client) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClient(socket: LocalSocket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val writer = PrintWriter(socket.outputStream, true)
            val command = reader.readLine() ?: return

            when {
                command == "PING" -> {
                    writer.println("PONG:${Process.myUid()}")
                }
                command.startsWith("EXEC:") -> {
                    val shellCmd = command.removePrefix("EXEC:")
                    val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", shellCmd))
                    val out = proc.inputStream.bufferedReader().readText()
                    val err = proc.errorStream.bufferedReader().readText()
                    proc.waitFor()
                    val combined = buildString {
                        if (out.isNotEmpty()) append(out)
                        if (err.isNotEmpty()) {
                            if (isNotEmpty()) append("\n")
                            append(err)
                        }
                    }
                    writer.println(combined)
                    writer.println("<<EOF>>")
                }
            }
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
