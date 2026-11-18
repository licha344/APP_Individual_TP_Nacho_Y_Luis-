# LessionTPNL

**Versión: 0.1.2**

Aplicación Android para monitorear la activación muscular mediante un sensor EMG conectado a una placa ESP32 por Bluetooth Low Energy (BLE). También incluye un simulador para probar la aplicación sin hardware.

## Funcionalidades

- Muestra la activación muscular estimada, la señal EMG bruta y la amplitud pico calculada.
- Busca dispositivos BLE ESP32 LESSION y recibe notificaciones del sensor EMG.
- Interpreta el protocolo de texto del firmware a 50 Hz y conserva compatibilidad con mensajes JSON anteriores.
- El conector BLE incluye soporte para comandos opcionales `START` y `STOP`.
- Permite simular mediciones, registrar sesiones y consultar el historial local.
- Exporta las mediciones y sesiones a un archivo CSV.
- Grafica la evolución de las métricas EMG y muestra las presiones sistólica y diastólica como series separadas.

## Requisitos

- Android Studio y JDK 25.
- Android SDK Platform 37.
- Dispositivo con Android 8.0 (API 26) o superior.
- Para BLE, una placa ESP32 con el sensor EMG conectado. El simulador no requiere hardware.

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

## Protocolo BLE

La aplicación se conecta al servicio UART BLE del ESP32 LESSION y recibe las mediciones en la característica TX:

| Elemento | UUID |
|---|---|
| Servicio GATT | `6E400001-B5A3-F393-E0A9-E50E24DCCA9E` |
| Característica TX (ESP32 → app) | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` |
| Característica RX (app → ESP32) | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` |
| Descriptor CCCD | `00002902-0000-1000-8000-00805f9b34fb` |

El firmware transmite una línea ASCII cada 20 ms con el formato `t_ms,adc\n`, donde `t_ms` son los milisegundos desde el arranque y `adc` es una lectura de 12 bits entre 0 y 4095. Ejemplo:

```text
12340,2048
```

La aplicación calcula las métricas EMG a partir de la lectura ADC, reensambla notificaciones fragmentadas y admite mensajes JSON anteriores. El canal RX permite enviar los comandos opcionales `START` y `STOP` para controlar la transmisión. Los permisos Bluetooth se solicitan según la versión de Android.

## Tecnologías

- Kotlin, Jetpack Compose y Material 3.
- Android Bluetooth GATT.
- Coroutines y StateFlow.
- Persistencia local y pruebas unitarias JVM.

## Nota

La señal EMG y los valores del simulador son herramientas de monitoreo y no reemplazan un diagnóstico ni una evaluación médica profesional. La pantalla principal prioriza las métricas EMG; algunas opciones secundarias ya no se muestran allí.
