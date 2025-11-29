package com.printerservice

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@ReactModule(name = PrinterServiceModule.NAME)
class PrinterServiceModule(private val reactContext: ReactApplicationContext) :
  NativePrinterServiceSpec(reactContext) {

  private var connection: PrinterConnection? = null
  private val scope = CoroutineScope(Job() + Dispatchers.IO)

  override fun getName(): String {
    return NAME
  }

  private fun sendEvent(eventName: String, params: WritableMap?) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  override fun connectTCP(host: String, port: Double, promise: Promise) {
    scope.launch {
      try {
        disconnectIfNeeded()
        connection = TcpPrinter(host, port.toInt()) { name, data -> onNativeEvent(name, data) }
        val ok = connection!!.connect()
        promise.resolve(ok)
      } catch (e: Exception) {
        promise.reject("connect_error", e)
      }
    }
  }

  @ReactMethod
  override fun connectBT(mac: String, promise: Promise) {
    scope.launch {
      try {
        disconnectIfNeeded()
        connection = BtPrinter(mac) { name, data -> onNativeEvent(name, data) }
        val ok = connection!!.connect()
        promise.resolve(ok)
      } catch (e: Exception) {
        promise.reject("connect_error", e)
      }
    }
  }

  @ReactMethod
  override fun connectUSB(promise: Promise) {
    scope.launch {
      try {
        disconnectIfNeeded()
        val mgr = reactContext.getSystemService(Context.USB_SERVICE) as UsbManager
        val dev: UsbDevice? = mgr.deviceList.values.firstOrNull()
        if (dev == null) {
          promise.resolve(false)
          return@launch
        }
        connection = UsbPrinter(mgr, dev) { name, data -> onNativeEvent(name, data) }
        val ok = connection!!.connect()
        promise.resolve(ok)
      } catch (e: Exception) {
        promise.reject("connect_error", e)
      }
    }
  }

  @ReactMethod
  override fun disconnect(promise: Promise) {
    scope.launch {
      try {
        connection?.disconnect()
        connection = null
        promise.resolve(true)
      } catch (e: Exception) {
        promise.reject("disconnect_error", e)
      }
    }
  }

  @ReactMethod
  override fun send(base64: String, promise: Promise) {
    scope.launch {
      try {
        val data = Base64.decode(base64, Base64.DEFAULT)
        val ok = connection?.send(data) ?: false
        promise.resolve(ok)
      } catch (e: Exception) {
        promise.reject("send_error", e)
      }
    }
  }

  @ReactMethod
  override fun pollStatus(promise: Promise) {
    scope.launch {
      try {
        val s = connection?.pollStatus()
        if (s == null) promise.resolve(null) else promise.resolve(bytesToHex(s))
      } catch (e: Exception) {
        promise.reject("status_error", e)
      }
    }
  }

  private fun disconnectIfNeeded() {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        connection?.disconnect()
      } catch (e: Exception) {
        Log.d("PrinterService", "disconnectIfNeeded: $e")
      }
    }
  }

  private fun onNativeEvent(name: String, data: String?) {
    val map: WritableMap = Arguments.createMap()
    map.putString("type", name)
    if (data != null) map.putString("data", data)
    sendEvent("PrinterEvent", map)
  }

  private fun bytesToHex(bytes: ByteArray): String {
    return bytes.joinToString(separator = "") { String.format("%02X", it) }
  }

  @ReactMethod
  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  companion object {
    const val NAME = "PrinterService"
  }
}
