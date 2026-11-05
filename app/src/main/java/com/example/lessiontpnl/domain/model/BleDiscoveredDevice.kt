package com.example.lessiontpnl.domain.model

/**
 * Modelo para representar un dispositivo BLE descubierto durante el escaneo.
 */
data class BleDiscoveredDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val timestamp: Long = System.currentTimeMillis()
)
