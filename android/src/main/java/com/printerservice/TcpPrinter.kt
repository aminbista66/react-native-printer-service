package com.printerservice

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class TcpPrinter(private val host: String, private val port: Int = 9100, private val cb: NativeEventCallback? = null) : PrinterConnection {
    private var socket: Socket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    constructor(host: String, port: Int, callback: (String, String?) -> Unit) : this(host, port, object : NativeEventCallback {
        override fun onEvent(name: String, data: String?) { callback(name, data) }
    })

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            socket = Socket()
            socket!!.connect(InetSocketAddress(host, port), 3000)
            socket!!.tcpNoDelay = true
            input = socket!!.getInputStream()
            output = socket!!.getOutputStream()
            cb?.onEvent("connected", "$host:$port")
            true
        } catch (e: Exception) {
            cb?.onEvent("error", e.message)
            false
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                input?.close()
                output?.close()
                socket?.close()
                cb?.onEvent("disconnected", "$host:$port")
            } catch (_: Exception) {}
        }
    }

    override suspend fun send(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (output == null) {
              cb?.onEvent("error", "Output stream is null. Connection not established.")
              return@withContext false
            }
            output?.write(data + byteArrayOf(0x0A))
            output?.flush()
            cb?.onEvent("printed", "${data.size} bytes")
            true
        } catch (e: Exception) {
            Log.e("TCP", "Send error", e)
            cb?.onEvent("error", e.message)
            false
        }
    }

    override suspend fun pollStatus(): ByteArray? = withContext(Dispatchers.IO) {
        return@withContext try {
            output?.write(byteArrayOf(0x10, 0x04, 0x01))
            output?.flush()
            val buffer = ByteArray(1)
            val r = input?.read(buffer)
            if (r != null && r > 0) {
                cb?.onEvent("status", "${buffer[0].toInt()}")
                buffer
            } else null
        } catch (e: Exception) {
            cb?.onEvent("error", e.message)
            null
        }
    }
}
