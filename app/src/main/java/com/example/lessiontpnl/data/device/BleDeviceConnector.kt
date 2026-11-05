package com.example.lessiontpnl.data.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import com.example.lessiontpnl.config.ProvisionalDataConfig
import com.example.lessiontpnl.domain.device.DeviceConnector
import com.example.lessiontpnl.domain.model.BleDiscoveredDevice
import com.example.lessiontpnl.domain.model.ConnectionState
import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.util.BlePermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Conector físico BLE para búsqueda, selección y comunicación GATT con sensores EMG/ESP32.
 * Soporta escaneo público de dispositivos cercanos con nombre, dirección MAC y RSSI,
 * permitiendo al usuario seleccionar explícitamente a qué dispositivo conectarse.
 */
class BleDeviceConnector(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
) : DeviceConnector {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _measurementFlow = MutableSharedFlow<MeasurementRecord>()
    override val measurementFlow: SharedFlow<MeasurementRecord> = _measurementFlow.asSharedFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDiscoveredDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BleDiscoveredDevice>> = _scannedDevices.asStateFlow()

    private val payloadParser = BlePayloadParser()
    private val discoveredDevicesMap = mutableMapOf<String, BleDiscoveredDevice>()

    private var bluetoothGatt: BluetoothGatt? = null
    private var measurementCharacteristic: BluetoothGattCharacteristic? = null
    private var scanJob: Job? = null
    private var isScanning = false
    @Volatile
    private var disconnectRequested = false

    private val serviceUuid = UUID.fromString(ProvisionalDataConfig.ESP32_SERVICE_UUID)
    private val characteristicUuid = UUID.fromString(ProvisionalDataConfig.ESP32_CHARACTERISTIC_UUID)
    private val cccdUuid = UUID.fromString(ProvisionalDataConfig.CCCD_DESCRIPTOR_UUID)

    @SuppressLint("MissingPermission")
    override suspend fun connect() {
        startInteractiveScan()
    }

    @SuppressLint("MissingPermission")
    fun startInteractiveScan() {
        if (!BlePermissionHelper.hasPermissions(context)) {
            _connectionState.value = ConnectionState.PermissionRequired
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = ConnectionState.BluetoothDisabled
            return
        }

        if (isScanning) return

        disconnectRequested = false
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        measurementCharacteristic = null
        payloadParser.resetBuffer()
        _connectionState.value = ConnectionState.Scanning
        isScanning = true
        discoveredDevicesMap.clear()
        _scannedDevices.value = emptyList()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            val scanner = adapter.bluetoothLeScanner
                ?: throw IllegalStateException("El adaptador no dispone de un escáner BLE")
            scanner.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            isScanning = false
            _connectionState.value = ConnectionState.Error("No se pudo iniciar el escáner BLE: ${e.localizedMessage}")
            return
        }

        // Detener el escaneo automáticamente tras el tiempo límite
        scanJob?.cancel()
        scanJob = scope.launch {
            delay(ProvisionalDataConfig.BLE_SCAN_TIMEOUT_MS)
            if (isScanning) {
                stopScan()
                if (discoveredDevicesMap.isEmpty()) {
                    _connectionState.value = ConnectionState.Error("No se encontraron dispositivos BLE cercanos.")
                } else if (_connectionState.value is ConnectionState.Scanning) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToAddress(macAddress: String) {
        stopScan()
        disconnectRequested = false

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = ConnectionState.BluetoothDisabled
            return
        }

        try {
            val device = adapter.getRemoteDevice(macAddress)
            connectToGatt(device)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Dirección MAC de dispositivo no válida: $macAddress")
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        disconnectRequested = true
        stopScan()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        measurementCharacteristic = null
        payloadParser.resetBuffer()
        _connectionState.value = ConnectionState.Disconnected
    }

    @SuppressLint("MissingPermission")
    override suspend fun triggerSimulatedError(message: String) {
        disconnectRequested = true
        stopScan()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        measurementCharacteristic = null
        payloadParser.resetBuffer()
        _connectionState.value = ConnectionState.Error(message)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        if (isScanning) {
            isScanning = false
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (_: Exception) {}
        }
    }

    fun cancelInteractiveScan() {
        stopScan()
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val rawName = result.scanRecord?.deviceName ?: device.name
            val displayName = if (rawName.isNull_or_blank()) "Dispositivo Desconocido" else rawName
            val address = device.address ?: return
            val rssi = result.rssi

            val discovered = BleDiscoveredDevice(
                name = displayName,
                address = address,
                rssi = rssi
            )

            discoveredDevicesMap[address] = discovered
            _scannedDevices.value = discoveredDevicesMap.values.sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            _connectionState.value = ConnectionState.Error("Fallo de escaneo BLE ($errorCode)")
        }
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    @SuppressLint("MissingPermission")
    private fun connectToGatt(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.Connecting
        try {
            val gatt = device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
            if (gatt == null) {
                _connectionState.value = ConnectionState.Error("Android no pudo iniciar la conexión GATT.")
                return
            }
            bluetoothGatt = gatt
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Error al conectar GATT: ${e.localizedMessage}")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (gatt !== bluetoothGatt) {
                gatt?.close()
                return
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt?.close()
                bluetoothGatt = null
                measurementCharacteristic = null
                _connectionState.value = if (disconnectRequested) {
                    ConnectionState.Disconnected
                } else {
                    ConnectionState.Error("Se perdió la conexión BLE (fallo GATT $status).")
                }
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                val mtuRequested = gatt?.requestMtu(ProvisionalDataConfig.REQUESTED_MTU) ?: false
                if (!mtuRequested) {
                    gatt?.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt?.close()
                bluetoothGatt = null
                measurementCharacteristic = null
                payloadParser.resetBuffer()
                _connectionState.value = if (disconnectRequested) {
                    ConnectionState.Disconnected
                } else {
                    ConnectionState.Error("Se perdió la conexión con el ESP32.")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            gatt?.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (gatt !== bluetoothGatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.Error("Fallo al descubrir servicios GATT")
                return
            }

            val service = gatt?.getService(serviceUuid)
            if (service == null) {
                _connectionState.value = ConnectionState.Error("El dispositivo no ofrece el servicio BLE configurado para el ESP32.")
                return
            }
            val characteristic = service.getCharacteristic(characteristicUuid)
            if (characteristic == null) {
                _connectionState.value = ConnectionState.Error("No se encontró la característica BLE configurada para las mediciones.")
                return
            }

            val descriptor = characteristic.getDescriptor(cccdUuid)
            if (descriptor == null) {
                _connectionState.value = ConnectionState.Error("La característica del ESP32 no ofrece el descriptor de notificaciones requerido.")
                return
            }

            if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
                _connectionState.value = ConnectionState.Error("La característica BLE del ESP32 no admite notificaciones.")
                return
            }
            measurementCharacteristic = characteristic
            val notificationSet = gatt.setCharacteristicNotification(characteristic, true)
            if (!notificationSet) {
                _connectionState.value = ConnectionState.Error("No se habilitaron las notificaciones en la característica.")
                return
            }

            val writeStarted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==
                    BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
            if (!writeStarted) {
                _connectionState.value = ConnectionState.Error("Android no pudo solicitar la suscripción BLE del ESP32.")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (gatt !== bluetoothGatt || descriptor.uuid != cccdUuid) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.Error("No se pudo activar la recepción de notificaciones del ESP32 ($status).")
                return
            }
            val characteristic = measurementCharacteristic
            if (characteristic != null &&
                characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.readCharacteristic(characteristic)
                } else {
                    @Suppress("DEPRECATION")
                    gatt.readCharacteristic(characteristic)
                }
            }
            _connectionState.value = ConnectionState.Connected
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                @Suppress("DEPRECATION")
                val bytes = characteristic.value ?: return
                handleIncomingBytes(bytes)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleIncomingBytes(value)
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic != null) {
                @Suppress("DEPRECATION")
                val bytes = characteristic.value ?: return
                handleIncomingBytes(bytes)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingBytes(value)
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray) {
        val records = payloadParser.processNotificationBytes(bytes)
        records.forEach { record ->
            scope.launch {
                _measurementFlow.emit(record)
            }
        }
    }
}
