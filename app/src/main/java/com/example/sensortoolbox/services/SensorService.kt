package com.example.sensortoolbox.services

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder

/** Streams readings from available motion and environmental sensors. */
class SensorService : Service(), SensorEventListener {
    data class Reading(
        val type: Int,
        val values: List<Float>,
        val accuracy: Int,
        val timestampNanos: Long
    )

    inner class LocalBinder : Binder() {
        fun getService(): SensorService = this@SensorService
    }

    private val binder = LocalBinder()
    private val readings = mutableMapOf<Int, Reading>()
    private val listeners = mutableSetOf<(Reading) -> Unit>()
    private lateinit var sensorManager: SensorManager

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        listOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_RELATIVE_HUMIDITY,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_PRESSURE
        ).forEach { type ->
            sensorManager.getDefaultSensor(type)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    /** Returns the latest reading for each available sensor that has reported. */
    fun getReadings(): Map<Int, Reading> = synchronized(readings) { readings.toMap() }

    fun isSensorAvailable(type: Int): Boolean = sensorManager.getDefaultSensor(type) != null

    fun addListener(listener: (Reading) -> Unit) {
        val current = synchronized(readings) {
            listeners.add(listener)
            readings.values.toList()
        }
        current.forEach(listener)
    }

    fun removeListener(listener: (Reading) -> Unit) {
        synchronized(readings) { listeners.remove(listener) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val reading = Reading(
            event.sensor.type,
            event.values.copyOf().toList(),
            event.accuracy,
            event.timestamp
        )
        val callbacks = synchronized(readings) {
            readings[event.sensor.type] = reading
            listeners.toList()
        }
        callbacks.forEach { it(reading) }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        val reading = synchronized(readings) {
            val previous = readings[sensor.type] ?: return
            previous.copy(accuracy = accuracy).also { readings[sensor.type] = it }
        }
        val callbacks = synchronized(readings) { listeners.toList() }
        callbacks.forEach { it(reading) }
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        synchronized(readings) {
            readings.clear()
            listeners.clear()
        }
        super.onDestroy()
    }
}