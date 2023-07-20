package kr.ac.hallym.healthlogger.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import kr.ac.hallym.healthlogger.Action
import kr.ac.hallym.healthlogger.listeners.HeartrateListener
import kr.ac.hallym.healthlogger.listeners.ISensorListener
import kr.ac.hallym.healthlogger.listeners.SPO2Listener
import kr.ac.hallym.healthlogger.statuses.HeartrateStatus
import kr.ac.hallym.healthlogger.toolkit.IDToolkit
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HealthTrackingAndroidService : Service() {
    lateinit var healthTracking: HealthTrackingService
    val notificationId = 1024
    lateinit var accFile: File
    lateinit var gyroFile: File
    lateinit var heartrateFile: File
    private var isLogging = false

    private fun makeForeground() {
        val channelId = javaClass.name
        val channel = NotificationChannel(
            channelId, "htas-channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification: Notification = Notification.Builder(this, channelId)
            .build()
        startForeground(1024, notification)
    }

    private fun getID(): String = IDToolkit.getID(filesDir.path)

    private fun initBroadcastReceiver() {
        val intentFilter = IntentFilter()
        Action.values()
            .filter { it.isFor(this.javaClass) }
            .forEach { intentFilter.addAction(it.name) }

        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(object: BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when(enumValueOf<Action>(intent.action!!)) {
                        Action.START_LOGGING -> {
                            Log.d("broadcasted", Action.START_LOGGING.name)
                            isLogging = true
                            val time = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
                            accFile = File(filesDir.path, "${getID()}_acc_${time}.csv")
                            accFile.writeText("time,ax,ay,az\n")
                            gyroFile = File(filesDir.path, "${getID()}_gyro_${time}.csv")
                            gyroFile.writeText("time,gx,gy,gz\n")
                            heartrateFile = File(filesDir.path, "${getID()}_heartrate_${time}")
                            heartrateFile.writeText("time,heartrate,status\n")
                        }
                        Action.END_LOGGING -> {
                            Log.d("broadcasted", Action.END_LOGGING.name)
                            isLogging = false
                        }
                        else -> {}
                    }
                }
            }, intentFilter)
    }

    override fun onCreate() {
        super.onCreate()
        initBroadcastReceiver()
    }

    inner class IMUListener(private val file: File, private val type: Int) : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!isLogging || event.sensor.type != type) return
            val line = System.currentTimeMillis().toString() + "," +
                    event.values.joinToString(",")
            file.appendText(line + "\n")
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val powerMgr = getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HEALTH_WAKELOCK")
        wakeLock.acquire()
        makeForeground()

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            IMUListener(accFile, Sensor.TYPE_ACCELEROMETER),
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI)

        sensorManager.registerListener(
            IMUListener(gyroFile, Sensor.TYPE_GYROSCOPE),
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_UI)


        healthTracking = HealthTrackingService(object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.d("HTService", "onConnectionSuccess called")

                val hrTracker = healthTracking.getHealthTracker(HealthTrackerType.HEART_RATE)
                hrTracker.setEventListener(object: HealthTracker.TrackerEventListener, ISensorListener {
                    private var value: Int = 0
                    fun getValue() = value

                    override fun onDataReceived(p0: MutableList<DataPoint>) {
                        p0.forEach {
                            it.getValue(ValueKey.HeartRateSet.HEART_RATE)
                            if (HeartrateStatus.from(it) != HeartrateStatus.SUCCESSFUL) return
                            Log.d(
                                javaClass.simpleName,
                                "${it.timestamp},${it.getValue(ValueKey.HeartRateSet.HEART_RATE)}"
                            )
                        }
                        value = p0.last().getValue(ValueKey.HeartRateSet.HEART_RATE)

                        LocalBroadcastManager
                            .getInstance(this@HealthTrackingAndroidService)
                            .sendBroadcastSync(Intent(Action.DATA_RECEIVED.toString())
                                .putExtra("lastHeartrate", value))
                    }

                    override fun onFlushCompleted() {
                    }

                    override fun onError(p0: HealthTracker.TrackerError?) {
                        Log.d("HR", p0.toString())
                    }
                })

                val ecgTracker = healthTracking.getHealthTracker(HealthTrackerType.ECG)
//                ecgTracker.setEventListener(ECGListener)

                val spO2Tracker = healthTracking.getHealthTracker(HealthTrackerType.SPO2)
//                spO2Tracker.setEventListener(SPO2Listener)
            }

            override fun onConnectionEnded() {
            }

            override fun onConnectionFailed(p0: HealthTrackerException?) {
                Log.d("HTService", p0.toString())
            }
        }, this)
        SPO2Listener.healthTrackingService = healthTracking
        healthTracking.connectService()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        healthTracking.disconnectService()
    }
}