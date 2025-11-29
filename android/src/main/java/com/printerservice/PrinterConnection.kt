package com.printerservice

interface NativeEventCallback {
    fun onEvent(name: String, data: String?)
}

interface PrinterConnection {
    suspend fun connect(): Boolean
    suspend fun disconnect()
    suspend fun send(data: ByteArray): Boolean
    suspend fun pollStatus(): ByteArray?
}
