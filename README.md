# LessionTPNL

Aplicación Android para monitorear el estado físico y la fatiga durante el entrenamiento mediante sensores BLE y métricas en tiempo real. La app permite conectarse a un ESP32/EMG, visualizar indicadores de rendimiento, registrar sesiones, detectar riesgos de fatiga y exportar el historial para análisis posterior.

## Descripción del proyecto

LessionTPNL es una solución para seguimiento de esfuerzo físico que combina:

- Conexión BLE con dispositivos compatibles para recibir señales del sensor EMG y otros datos de entrenamiento.
- Simulación de datos para pruebas y demostración sin hardware físico.
- Visualización de métricas clave: frecuencia cardiaca, temperatura corporal, presión arterial y activación muscular.
- Registro de sesiones de esfuerzo con cálculo de mínimos, máximos y tendencias de fatiga.
- Gráficos tipo box plot y de progreso temporal para analizar cambios durante la sesión.
- Alertas de riesgo y configuración de umbrales personalizados.
- Persistencia local del historial y exportación a CSV.

La interfaz está construida con Kotlin y Jetpack Compose, usando Material 3 y StateFlow para reaccionar a cambios de estado en tiempo real.

## Funcionalidades principales

- Escaneo y conexión a dispositivos BLE.
- Modo simulador para pruebas locales.
- Mostrar métricas de rendimiento en pantalla.
- Iniciar, pausar, detener y reiniciar sesiones.
- Guardar historial persistente en almacenamiento interno.
- Exportar datos a archivos CSV.
- Evaluación visual de riesgo de fatiga muscular.

## Stack tecnológico

- Kotlin
- Jetpack Compose
- Material 3
- Android Bluetooth GATT
- Coroutines + Flow
- Gradle/Kotlin DSL

## Requisitos

- Android Studio
- JDK compatible con Gradle (configurado con el proyecto)
- Android SDK 37
- Dispositivo Android 8.0+ o emulador compatible

## Cómo ejecutar

```bash
./gradlew test
./gradlew assembleDebug
```

El APK generado quedará en la carpeta `app/build/outputs/apk/debug`.

## Nota

Esta aplicación está enfocada en apoyo a monitoreo de esfuerzo y fatiga; no reemplaza diagnóstico ni evaluación médica profesional.
**Versión: 0.0.1**

Aplicación Android para visualizar y registrar métricas físicas durante el entrenamiento. Recibe datos de un dispositivo ESP32 por Bluetooth Low Energy (BLE) o permite probar la interfaz con un simulador integrado.

## Funcionalidades

- Muestra en tiempo real la frecuencia cardíaca, la temperatura corporal, la presión arterial y la activación/fatiga muscular EMG.
- Busca dispositivos BLE cercanos, permite elegir uno y se conecta a su servicio GATT para leer notificaciones.
- Incluye un modo simulador que genera mediciones periódicas para desarrollo y demostración.
- Permite iniciar, detener y reiniciar sesiones de fatiga; calcula valores mínimos y máximos y muestra gráficos de caja y bigotes.
- Presenta gráficos históricos por métrica y rango de tiempo, y conserva mediciones y sesiones entre reinicios.
- Configura umbrales de riesgo de fatiga y genera alertas mediante notificaciones.
- Exporta el historial de mediciones y sesiones a un archivo CSV.

> **Nota sobre los datos:** los valores del simulador son sintéticos. En el protocolo ESP32 actual, únicamente EMG se identifica como medición física; frecuencia cardíaca, temperatura y presión recibidas se marcan como datos de demostración fijos. Esta aplicación no es un dispositivo médico y sus resultados no sustituyen una evaluación profesional.

## Requisitos

- Android Studio con JDK 25 (la versión configurada para Gradle en este proyecto).
- Android SDK Platform 37 para compilar.
- Dispositivo con Android 8.0 (API 26) o superior para ejecutar.
- Para el modo BLE: Bluetooth Low Energy y un ESP32 configurado con el servicio y característica indicados abajo. El modo simulador no necesita hardware.

## Compilar y probar

Desde la raíz del proyecto:

```shell
.\gradlew.bat test
```

Para generar el APK de depuración:

```shell
.\gradlew.bat assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/`.

## Protocolo BLE del ESP32

La aplicación busca dispositivos BLE, permite seleccionar el dispositivo y se suscribe a la característica configurada:

| Elemento | UUID |
|---|---|
| Servicio GATT | `4fafc201-1fb5-459e-8fcc-c5c9c331914b` |
| Característica de mediciones | `beb5483e-36e1-4688-b7f5-ea07361b26a8` |
| Descriptor CCCD | `00002902-0000-1000-8000-00805f9b34fb` |

El payload esperado es un objeto JSON con campos numéricos opcionales: `emg` (valor bruto EMG), `fatigue` (activación entre 0 y 100), `hr` (frecuencia cardíaca), `temp` (temperatura), `sys` y `dia` (presión sistólica y diastólica). Por ejemplo:

```json
{"emg":1200,"fatigue":40,"hr":90,"temp":36.8,"sys":120,"dia":80}
```

Los fragmentos JSON recibidos mediante notificaciones BLE se reensamblan antes de procesarse. Los permisos de Bluetooth se solicitan según la versión de Android.

## Tecnologías

- Kotlin y Jetpack Compose con Material 3.
- Android Bluetooth GATT para comunicación BLE.
- Coroutines y StateFlow para el flujo de mediciones y estado de interfaz.
- Persistencia local en almacenamiento interno y preferencias de Android.
- Pruebas unitarias JVM y pruebas instrumentadas.

## Estructura

- `app/src/main/java/.../data/device`: conectores BLE y de simulación, y parser de payloads.
- `app/src/main/java/.../data/repository`: persistencia del historial y sesiones.
- `app/src/main/java/.../domain`: modelos y contratos del dominio.
- `app/src/main/java/.../ui`: pantalla, componentes Compose y ViewModel.
- `app/src/main/java/.../util`: permisos, cálculos, exportación CSV y notificaciones.

## Versión 0.0.1

Primera versión del proyecto con visualización de métricas, conexión BLE ESP32, simulación de datos, registro de sesiones de fatiga, historial persistente, gráficos, alertas y exportación CSV.
