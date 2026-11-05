package com.example.lessiontpnl.domain.model

/**
 * Configuración de umbrales de riesgo definidos por el usuario para la sesión de monitoreo de fatiga.
 */
data class FatigueRiskThresholds(
    val activationPercent: Int? = null,
    val emgRaw: Int? = null,
    val heartRateBpm: Int? = null,
    val tempCelsius: Double? = null
) {
    fun validationError(): String? {
        if (activationPercent != null && activationPercent !in 0..100) {
            return "El umbral de fatiga debe estar entre 0 y 100 %."
        }
        if (emgRaw != null && emgRaw < 0) {
            return "El umbral EMG debe ser cero o mayor."
        }
        if (heartRateBpm != null && heartRateBpm !in 30..250) {
            return "El umbral de ritmo cardíaco (bpm) debe estar entre 30 y 250."
        }
        if (tempCelsius != null && tempCelsius !in 30.0..45.0) {
            return "El umbral de temperatura (°C) debe estar entre 30.0 y 45.0."
        }
        return null
    }

    fun exceededParameters(
        currentActivation: Int?,
        currentEmg: Int?,
        currentHr: Int? = null,
        currentTemp: Double? = null
    ): List<String> = buildList {
        if (activationPercent != null && currentActivation != null && currentActivation > activationPercent) {
            add("fatiga: $currentActivation % (umbral $activationPercent %)")
        }
        if (emgRaw != null && currentEmg != null && currentEmg > emgRaw) {
            add("señal EMG: $currentEmg unidades (umbral $emgRaw unidades)")
        }
        if (heartRateBpm != null && currentHr != null && currentHr > heartRateBpm) {
            add("ritmo cardíaco: $currentHr bpm (umbral $heartRateBpm bpm)")
        }
        if (tempCelsius != null && currentTemp != null && currentTemp > tempCelsius) {
            add("temperatura corporal: $currentTemp °C (umbral $tempCelsius °C)")
        }
    }

    fun exceededParameters(activation: Int, rawEmg: Int): List<String> {
        return exceededParameters(
            currentActivation = activation,
            currentEmg = rawEmg,
            currentHr = null,
            currentTemp = null
        )
    }
}
