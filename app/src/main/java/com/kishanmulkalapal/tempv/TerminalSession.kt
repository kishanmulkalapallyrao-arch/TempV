package com.kishanmulkalapal.tempv

import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

class TerminalSession(
    private val shellPath: String,
    private val workDir: String,
    private val onOutput: (ByteArray) -> Unit,
    private val onExit: () -> Unit
) {
    private var masterFd = -1
    private var input: FileOutputStream? = null
    private var output: FileInputStream? = null

    @Volatile private var running = false

    fun start(rows: Int, cols: Int) {
        masterFd = Pty.createSubprocess(shellPath, workDir, rows, cols)
        if (masterFd < 0) { onExit(); return }

        val pfd = ParcelFileDescriptor.adoptFd(masterFd)
        val writeSide = pfd.dup()

        output = ParcelFileDescriptor.AutoCloseInputStream(pfd)
        input = ParcelFileDescriptor.AutoCloseOutputStream(writeSide)

        running = true
        Thread {
            val buf = ByteArray(8192)
            try {
                while (running) {
                    val n = output!!.read(buf)
                    if (n <= 0) break
                    onOutput(buf.copyOf(n))
                }
            } catch (_: Exception) {
            } finally {
                running = false
                onExit()
            }
        }.apply { isDaemon = true }.start()
    }

    fun write(data: ByteArray) {
        try { input?.write(data); input?.flush() } catch (_: Exception) {}
    }

    fun write(text: String) = write(text.toByteArray(Charsets.UTF_8))

    fun resize(rows: Int, cols: Int) {
        if (masterFd >= 0) Pty.resize(masterFd, rows, cols)
    }

    fun stop() {
        running = false
        try { input?.close() } catch (_: Exception) {}
        try { output?.close() } catch (_: Exception) {}
    }
}
