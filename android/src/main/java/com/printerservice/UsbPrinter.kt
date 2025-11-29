package com.printerservice

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsbPrinter(private val manager: UsbManager, private val device: UsbDevice, private val cb: NativeEventCallback? = null) : PrinterConnection {
    private var connection: UsbDeviceConnection? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null

    constructor(mgr: UsbManager, dev: UsbDevice, callback: (String, String?) -> Unit) : this(mgr, dev, object : NativeEventCallback {
        override fun onEvent(name: String, data: String?) { callback(name, data) }
    })

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!manager.hasPermission(device)) {
                cb?.onEvent("permission_required", null)
                return@withContext false
            }
            connection = manager.openDevice(device) ?: return@withContext false
            val intf = device.getInterface(0)
            connection!!.claimInterface(intf, true)
            for (i in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(i)
                if (ep.direction == UsbConstants.USB_DIR_OUT) endpointOut = ep
                if (ep.direction == UsbConstants.USB_DIR_IN) endpointIn = ep
            }
            cb?.onEvent("connected", "usb:${device.deviceId}")
            return@withContext endpointOut != null
        } catch (e: Exception) {
            cb?.onEvent("error", e.message)
            return@withContext false
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                connection?.close()
                cb?.onEvent("disconnected", "usb:${device.deviceId}")
            } catch (_: Exception) {}
        }
    }

    override suspend fun send(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val sent = connection?.bulkTransfer(endpointOut, data, data.size, 5000)
            cb?.onEvent("printed", "${sent ?: -1} bytes")
            return@withContext sent != null && sent > 0
        } catch (e: Exception) {
            cb?.onEvent("error", e.message)
            return@withContext false
        }
    }

    override suspend fun pollStatus(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val cmd = byteArrayOf(0x10, 0x04, 0x01)
            connection?.bulkTransfer(endpointOut, cmd, cmd.size, 1000)
            val buffer = ByteArray(1)
            val r = connection?.bulkTransfer(endpointIn, buffer, 1, 2000)
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
