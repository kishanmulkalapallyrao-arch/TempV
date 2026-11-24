package com.kishanmulkalapal.tempv

object Pty {
    init { System.loadLibrary("term") }
    external fun createSubprocess(cmd: String, cwd: String, rows: Int, cols: Int): Int
    external fun resize(fd: Int, rows: Int, cols: Int)
}
