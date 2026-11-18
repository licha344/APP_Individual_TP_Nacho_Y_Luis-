package com.example.lessiontpnl.config

/**
 * Centralización de valores de configuración para métricas, simulación y protocolo Bluetooth BLE (ESP32 LESSION).
 */
object ProvisionalDataConfig {
    /** Unidad de medida para Temperatura Corporal */
    const val BODY_TEMP_UNIT = "°C"

    /** Unidad de medida para Presión Arterial */
    const val BLOOD_PRESSURE_UNIT = "mmHg"

    /** Estado y mensaje por defecto para Fatiga Muscular */
    const val MUSCLE_FATIGUE_STATUS = "Sin datos"
    const val MUSCLE_FATIGUE_UNAVAILABLE_REASON = "Sin señal de sensor EMG conectada."

    /** Escala o unidad para Activación Muscular / EMG */
    const val MUSCLE_FATIGUE_SCALE = "% Activación"

    /** Intervalo de emisión de datos simulados en milisegundos */
    const val SIMULATION_INTERVAL_MS = 3000L

    /** Tiempo de retardo para simular el handshake de conexión en ms */
    const val CONNECTION_DELAY_MS = 1000L

    // --- PROTOCOLO ESP32 LESSION BLE GATT (Nordic UART Service Style) ---
    /** Nombre anunciado por el servidor GATT ESP32 en ESP32_LESSION.ino */
    const val ESP32_DEVICE_NAME = "ESP32 LESSION"

    /** UUID del Servicio GATT anunciado por el ESP32 */
    const val ESP32_SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"

    /** UUID de la Característica TX (ESP32 -> App, READ + NOTIFY) */
    const val ESP32_CHARACTERISTIC_UUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

    /** UUID de la Característica RX (App -> ESP32, WRITE) */
    const val ESP32_RX_CHARACTERISTIC_UUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"

    /** UUID estándar del Descriptor CCCD para suscripción a notificaciones */
    const val CCCD_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

    /** Límite de tiempo máximo para el escaneo BLE en milisegundos */
    const val BLE_SCAN_TIMEOUT_MS = 12000L

    /** Solicitud de MTU objetivo para recepción de notificaciones */
    const val REQUESTED_MTU = 512
}
