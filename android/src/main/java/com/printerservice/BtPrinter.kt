package com.printerservice

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BtPrinter(private val macAddress: String, private val cb: NativeEventCallback? = null) : PrinterConnection {
    private val adapter = BluetoothAdapter.getDefaultAdapter()
    private var device: BluetoothDevice? = null
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    constructor(mac: String, callback: (String, String?) -> Unit) : this(mac, object : NativeEventCallback {
        override fun onEvent(name: String, data: String?) { callback(name, data) }
    })

    @SuppressLint("MissingPermission")
    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            device = adapter.getRemoteDevice(macAddress)
            socket = device!!.createRfcommSocketToServiceRecord(SPP_UUID)
            adapter.cancelDiscovery()
            socket!!.connect()
            input = socket!!.inputStream
            output = socket!!.outputStream
            cb?.onEvent("connected", macAddress)
            true
        } catch (e: SecurityException) {
            cb?.onEvent("error", "SecurityException: ${e.message}")
            false
        } catch (e: Exception) {
            cb?.onEvent("error", e.message)
            false
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                socket?.close()
                cb?.onEvent("disconnected", macAddress)
            } catch (_: Exception) {}
        }
    }

    override suspend fun send(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            output?.write(data)
            output?.flush()
            cb?.onEvent("printed", "${data.size} bytes")
            true
        } catch (e: Exception) {
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
