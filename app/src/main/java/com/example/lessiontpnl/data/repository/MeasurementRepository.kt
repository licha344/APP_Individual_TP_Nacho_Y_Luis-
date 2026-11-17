package com.example.lessiontpnl.data.repository

import android.content.Context
import com.example.lessiontpnl.domain.model.DataSource
import com.example.lessiontpnl.domain.model.FatigueSession
import com.example.lessiontpnl.domain.model.MeasurementRecord
import com.example.lessiontpnl.domain.model.MetricType
import com.example.lessiontpnl.domain.model.MetricValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

/**
 * Repositorio central de almacenamiento de mediciones con persistencia física en almacenamiento interno.
 * Garantiza que las mediciones permanezcan guardadas aunque la aplicación se cierre o reinicie.
 * Mantiene las mediciones ordenadas desde la más reciente hasta la más antigua.
 */
class MeasurementRepository(
    private val context: Context? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val historyFile: File? = context?.let { File(it.filesDir, "measurement_history.json") }
    private val internalList = mutableListOf<MeasurementRecord>()
    private val recordsLock = Any()
    private val initialization = CompletableDeferred<Unit>()
    private val persistenceMutex = Mutex()
    private var clearRequested = false

    private val _allRecords = MutableStateFlow<List<MeasurementRecord>>(emptyList())
    val allRecords: StateFlow<List<MeasurementRecord>> = _allRecords.asStateFlow()

    private val _latestRecordsByMetric = MutableStateFlow<Map<MetricType, MeasurementRecord>>(emptyMap())
    val latestRecordsByMetric: StateFlow<Map<MetricType, MeasurementRecord>> = _latestRecordsByMetric.asStateFlow()

    private val _storageError = MutableStateFlow<String?>(null)
    val storageError: StateFlow<String?> = _storageError.asStateFlow()

    private val _fatigueSessions = MutableStateFlow<List<FatigueSession>>(emptyList())
    val fatigueSessions: StateFlow<List<FatigueSession>> = _fatigueSessions.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    init {
        scope.launch {
            try {
                loadFromDisk()
            } finally {
                _isInitialized.value = true
                initialization.complete(Unit)
            }
        }
    }

    fun addRecord(record: MeasurementRecord) {
        synchronized(recordsLock) {
            internalList.add(record)
            publishRecords()
        }
        persistCurrentRecords()
    }

    fun saveFatigueSession(session: FatigueSession) {
        synchronized(recordsLock) {
            val updated = _fatigueSessions.value.filterNot { it.sessionId == session.sessionId } + session
            _fatigueSessions.value = updated.sortedByDescending { it.startTime }
        }
        persistCurrentRecords()
    }

    fun removeFatigueSession(sessionId: String) {
        synchronized(recordsLock) {
            _fatigueSessions.value = _fatigueSessions.value.filterNot { it.sessionId == sessionId }
        }
        persistCurrentRecords()
    }

    fun clearHistory() {
        synchronized(recordsLock) {
            clearRequested = true
            internalList.clear()
            _fatigueSessions.value = emptyList()
            publishRecords()
        }
        persistCurrentRecords()
    }

    private fun publishRecords() {
        val sortedList = internalList.sortedByDescending { it.timestamp }
        _allRecords.value = sortedList

        val latestMap = mutableMapOf<MetricType, MeasurementRecord>()
        sortedList.forEach { rec ->
            if (!latestMap.containsKey(rec.metricType)) {
                latestMap[rec.metricType] = rec
            }
        }
        _latestRecordsByMetric.value = latestMap
    }

    private fun persistCurrentRecords() {
        _storageError.value = null
        scope.launch {
            initialization.await()
            persistenceMutex.withLock {
                try {
                    val (records, sessions) = synchronized(recordsLock) {
                        internalList.sortedByDescending { it.timestamp } to _fatigueSessions.value
                    }
                    saveToDisk(records, sessions)
                } catch (e: Exception) {
                    _storageError.value = "No se pudo guardar el historial: ${e.localizedMessage ?: "error de almacenamiento"}"
                }
            }
        }
    }

    private fun loadFromDisk() {
        val file = historyFile ?: return

        try {
            val atomicFile = AtomicFile(file)
            val inputStream = try {
                atomicFile.openRead()
            } catch (_: FileNotFoundException) {
                return
            }
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (jsonString.isBlank()) return

            val root = jsonString.trimStart().firstOrNull()
            val loadedList = mutableListOf<MeasurementRecord>()
            val loadedSessions = mutableListOf<FatigueSession>()
            val recordsArray: JSONArray
            if (root == '[') {
                recordsArray = JSONArray(jsonString)
            } else {
                val jsonObject = JSONObject(jsonString)
                recordsArray = jsonObject.optJSONArray("records") ?: JSONArray()
                val sessionsArray = jsonObject.optJSONArray("sessions") ?: JSONArray()
                for (i in 0 until sessionsArray.length()) {
                    deserializeSession(sessionsArray.getJSONObject(i))?.let(loadedSessions::add)
                }
            }

            for (i in 0 until recordsArray.length()) {
                val record = deserializeRecord(recordsArray.getJSONObject(i))
                if (record != null) {
                    loadedList.add(record)
                }
            }

            synchronized(recordsLock) {
                if (!clearRequested) {
                    val existingIds = internalList.mapTo(mutableSetOf()) { it.id }
                    internalList.addAll(loadedList.filterNot { it.id in existingIds })
                    val sessionsById = loadedSessions.associateBy { it.sessionId }.toMutableMap()
                    _fatigueSessions.value.forEach { sessionsById[it.sessionId] = it }
                    _fatigueSessions.value = sessionsById.values.sortedByDescending { it.startTime }
                }
                publishRecords()
            }
        } catch (e: Exception) {
            _storageError.value = "No se pudo cargar el historial: ${e.localizedMessage ?: "archivo no válido"}"
        }
    }

    private fun saveToDisk(records: List<MeasurementRecord>, sessions: List<FatigueSession>) {
        val file = historyFile ?: return
        val recordsArray = JSONArray()
        records.forEach { record ->
            recordsArray.put(serializeRecord(record))
        }

        val sessionsArray = JSONArray()
        sessions.forEach { session ->
            sessionsArray.put(serializeSession(session))
        }

        val root = JSONObject()
            .put("records", recordsArray)
            .put("sessions", sessionsArray)
        val atomicFile = AtomicFile(file)
        var outputStream: java.io.FileOutputStream? = null
        try {
            outputStream = atomicFile.startWrite()
            outputStream.write(root.toString().toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(outputStream)
        } catch (e: Exception) {
            outputStream?.let { atomicFile.failWrite(it) }
            throw e
        }
    }

    private fun serializeRecord(record: MeasurementRecord): JSONObject {
        val obj = JSONObject()
        obj.put("id", record.id)
        obj.put("metricType", record.metricType.name)
        obj.put("timestamp", record.timestamp)
        obj.put("source", record.source.name)
        record.sessionId?.let { obj.put("sessionId", it) }

        when (val v = record.value) {
            is MetricValue.HeartRate -> {
                obj.put("valueType", "HeartRate")
                obj.put("val1", v.bpm)
            }
            is MetricValue.BodyTemperature -> {
                obj.put("valueType", "BodyTemperature")
                obj.put("valDouble", v.celsius)
            }
            is MetricValue.BloodPressure -> {
                obj.put("valueType", "BloodPressure")
                obj.put("val1", v.systolic)
                obj.put("val2", v.diastolic)
            }
            is MetricValue.MuscleFatigueEmg -> {
                obj.put("valueType", "MuscleFatigueEmg")
                obj.put("val1", v.emgRaw)
                obj.put("val2", v.activationPercent)
            }
            is MetricValue.EmgRawValue -> {
                obj.put("valueType", "EmgRawValue")
                obj.put("val1", v.rawValue)
            }
            is MetricValue.EmgPeakValue -> {
                obj.put("valueType", "EmgPeakValue")
                obj.put("valDouble", v.peakMv)
            }
            is MetricValue.MuscleFatigueUnavailable -> {
                obj.put("valueType", "MuscleFatigueUnavailable")
                obj.put("valStr", v.reason)
            }
            is MetricValue.Error -> {
                obj.put("valueType", "Error")
                obj.put("valStr", v.message)
            }
            is MetricValue.NoData -> {
                obj.put("valueType", "NoData")
            }
        }
        return obj
    }

    private fun deserializeRecord(obj: JSONObject): MeasurementRecord? {
        return try {
            val id = obj.getString("id")
            val metricType = MetricType.valueOf(obj.getString("metricType"))
            val timestamp = obj.getLong("timestamp")
            val source = DataSource.valueOf(obj.getString("source"))

            val valueType = obj.optString("valueType", "")
            val value: MetricValue = when (valueType) {
                "HeartRate" -> MetricValue.HeartRate(obj.getInt("val1"))
                "BodyTemperature" -> MetricValue.BodyTemperature(obj.getDouble("valDouble"))
                "BloodPressure" -> MetricValue.BloodPressure(obj.getInt("val1"), obj.getInt("val2"))
                "MuscleFatigueEmg" -> MetricValue.MuscleFatigueEmg(obj.getInt("val1"), obj.getInt("val2"))
                "EmgRawValue" -> MetricValue.EmgRawValue(obj.getInt("val1"))
                "EmgPeakValue" -> MetricValue.EmgPeakValue(obj.getDouble("valDouble"))
                "MuscleFatigueUnavailable" -> MetricValue.MuscleFatigueUnavailable(obj.optString("valStr", ""))
                "Error" -> MetricValue.Error(obj.optString("valStr", "Error"))
                else -> MetricValue.NoData
            }

            MeasurementRecord(
                id = id,
                metricType = metricType,
                value = value,
                timestamp = timestamp,
                source = source,
                sessionId = obj.optString("sessionId").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun serializeSession(session: FatigueSession): JSONObject {
        val samples = JSONArray()
        session.samples.forEach(samples::put)
        return JSONObject()
            .put("sessionId", session.sessionId)
            .put("startTime", session.startTime)
            .put("endTime", session.endTime ?: JSONObject.NULL)
            .put("currentPercent", session.currentPercent ?: JSONObject.NULL)
            .put("minPercent", session.minPercent ?: JSONObject.NULL)
            .put("maxPercent", session.maxPercent ?: JSONObject.NULL)
            .put("sampleCount", session.sampleCount)
            .put("isActive", session.isActive)
            .put("source", session.source?.name ?: JSONObject.NULL)
            .put("samples", samples)
    }

    private fun deserializeSession(obj: JSONObject): FatigueSession? {
        return try {
            val samplesArray = obj.optJSONArray("samples") ?: JSONArray()
            val samples = List(samplesArray.length()) { index -> samplesArray.getInt(index) }
            FatigueSession(
                sessionId = obj.getString("sessionId"),
                startTime = obj.getLong("startTime"),
                endTime = obj.optLong("endTime").takeUnless { obj.isNull("endTime") },
                currentPercent = obj.optInt("currentPercent").takeUnless { obj.isNull("currentPercent") },
                minPercent = obj.optInt("minPercent").takeUnless { obj.isNull("minPercent") },
                maxPercent = obj.optInt("maxPercent").takeUnless { obj.isNull("maxPercent") },
                sampleCount = obj.optInt("sampleCount", samples.size),
                isActive = obj.optBoolean("isActive", false),
                source = obj.optString("source").takeIf { it.isNotBlank() && it != "null" }?.let(DataSource::valueOf),
                samples = samples
            )
        } catch (_: Exception) {
            null
        }
    }
}
