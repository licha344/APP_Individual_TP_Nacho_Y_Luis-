# LessionTPNL

**Versión: 0.1.1**

Aplicación Android para monitorear la activación muscular mediante un sensor EMG conectado a una placa ESP32 por Bluetooth Low Energy (BLE). También incluye un simulador para probar la aplicación sin hardware.

## Funcionalidades

- Muestra la activación muscular estimada, la señal EMG bruta y la amplitud pico calculada.
- Busca dispositivos BLE cercanos y recibe notificaciones del sensor EMG.
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

La aplicación se suscribe a la característica configurada en el ESP32:

| Elemento | UUID |
|---|---|
| Servicio GATT | `4fafc201-1fb5-459e-8fcc-c5c9c331914b` |
| Característica de mediciones | `beb5483e-36e1-4688-b7f5-ea07361b26a8` |
| Descriptor CCCD | `00002902-0000-1000-8000-00805f9b34fb` |

El payload JSON debe incluir `emg` (valor bruto ADC) y `fatigue` (activación entre 0 y 100). Por ejemplo:

```json
{"emg":1200,"fatigue":40}
```

Las notificaciones fragmentadas se reensamblan antes de procesarse. Los permisos Bluetooth se solicitan según la versión de Android.

## Tecnologías

- Kotlin, Jetpack Compose y Material 3.
- Android Bluetooth GATT.
- Coroutines y StateFlow.
- Persistencia local y pruebas unitarias JVM.

## Nota

La señal EMG y los valores del simulador son herramientas de monitoreo y no reemplazan un diagnóstico ni una evaluación médica profesional. La pantalla principal prioriza las métricas EMG; algunas opciones secundarias ya no se muestran allí.
