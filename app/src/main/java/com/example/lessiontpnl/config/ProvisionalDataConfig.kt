package com.example.lessiontpnl.config

/**
 * Centralización de valores provisionales para métricas, simulación y Bluetooth BLE (ESP32).
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

    // --- PROTOCOLO ESP32 BLE GATT ---
    /** Nombre anunciado por el servidor GATT ESP32 */
    const val ESP32_DEVICE_NAME = "ESP32_EMG_MONITOR"

    /** UUID del Servicio GATT anunciado por el ESP32 */
    const val ESP32_SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"

    /** UUID de la Característica de Lectura/Notificación del ESP32 */
    const val ESP32_CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"

    /** UUID estándar del Descriptor CCCD para suscripción a notificaciones */
    const val CCCD_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

    /** Límite de tiempo máximo para el escaneo BLE en milisegundos */
    const val BLE_SCAN_TIMEOUT_MS = 12000L

    /** Solicitud de MTU objetivo para recepción completa de JSON */
    const val REQUESTED_MTU = 512
}
