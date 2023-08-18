package kr.ac.hallym.healthlogger

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.wear.ambient.AmbientModeSupport
import kr.ac.hallym.healthlogger.listeners.HeartrateListener
import kr.ac.hallym.healthlogger.services.HealthTrackingAndroidService
import kr.ac.hallym.healthlogger.toolkit.IDToolkit
import java.io.File
import kotlin.math.abs
import kotlin.random.Random
import kotlin.system.exitProcess


class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    lateinit var value: String

    private lateinit var togglebtn: ToggleButton
    private lateinit var textview: TextView
    private var isAmbientMode: Boolean = false

    private val newHeartrateViewThread = {
        Thread {
            while (true) {
                try {
                    textview.text = "${HeartrateListener.getValue()} bpm"
                    Thread.sleep(1000)
                } catch (ex: InterruptedException) {
                    break
                }
            }
        }
    }

    private lateinit var heartrateViewThreadPtr: Thread

    @Suppress("DEPRECATION")
    fun <T> isServiceRunning(service: Class<T>): Boolean {
        return (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any { it -> it.service.className == service.name }
    }

    private fun initBroadcastReceiver() {
        val intentFilter = IntentFilter()
        Action.values()
            .filter { it.isFor(this.javaClass) }
            .forEach { intentFilter.addAction(it.name) }

        LocalBroadcastManager
            .getInstance(applicationContext)
            .registerReceiver(object : BroadcastReceiver() {
                @SuppressLint("SetTextI18n")
                override fun onReceive(context: Context, intent: Intent) {
                    when (enumValueOf<Action>(intent.action!!)) {
                        Action.IS_LOGGING -> {
                            togglebtn.isChecked = true
                        }

                        Action.IS_NOT_LOGGING -> {
                            togglebtn.isChecked = false
                        }

                        Action.DATA_RECEIVED -> {
                            if (isAmbientMode) return
                            val value = intent.getIntExtra(IntentExtra.LAST_HEARTRATE.toString(), 0)
                            textview.text = "$value bpm"
                        }

                        Action.SEND_SUCCESSFUL -> {
                            runOnUiThread {
                                Toast.makeText(
                                    applicationContext,
                                    "Send completed",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }

                        Action.SEND_FAILED -> {
                            runOnUiThread {
                                Toast.makeText(
                                    applicationContext,
                                    "Send failed",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }

                        else -> {}
                    }
                }
            }, intentFilter)
    }

    private val isDenied = { p: String ->
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_DENIED
    }

    private fun reqPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
        )

        if (permissions.any(isDenied))
            requestPermissions(permissions.filter(isDenied).toTypedArray(), 1001)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.any(isDenied))
            reqPermissions()
    }

    private fun getID(): String = IDToolkit.getID(filesDir.path)

    @SuppressLint("InvalidWakeLockTag", "BatteryLife", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initBroadcastReceiver()
        reqPermissions()
        getID()

        togglebtn = findViewById(R.id.togglebtn)
        togglebtn.isEnabled = true

        textview = findViewById(R.id.textview)
        textview.text = "- bpm"

        val idview = findViewById<TextView>(R.id.idview)
        idview.text = IDToolkit.getID(filesDir.path)

        LocalBroadcastManager
            .getInstance(applicationContext)
            .sendBroadcastSync(Intent(Action.Q_IS_LOGGING.toString()))

//        heartrateViewThreadPtr = newHeartrateViewThread()
//        heartrateViewThreadPtr.start()

        togglebtn.setOnCheckedChangeListener { _, isChecked ->
            val broadcastManager = LocalBroadcastManager.getInstance(this)
            val intent = Intent()

            intent.action = if (isChecked) Action.START_LOGGING.name else Action.END_LOGGING.name
            broadcastManager.sendBroadcast(intent)
        }

        val powerMgr = getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WAKELOCK")
        wakeLock.acquire()

        if (!powerMgr.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, 0)
        }

        ambientController = AmbientModeSupport.attach(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (isServiceRunning(HealthTrackingAndroidService::class.java)) {
            Log.d(
                "ServiceRunning",
                "${HealthTrackingAndroidService::class.simpleName} is running."
            )
            return
        }
        val healthIntent = Intent(this, HealthTrackingAndroidService::class.java)
        applicationContext.startForegroundService(healthIntent)
    }

    private lateinit var ambientController: AmbientModeSupport.AmbientController

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback = MyAmbientCallback()

    private inner class MyAmbientCallback :
        AmbientModeSupport.AmbientCallback() {
        override fun onEnterAmbient(ambientDetails: Bundle?) {
            Log.d("MainActivity", "onEnterAmbient")
//            mainActivity.heartrateViewThreadPtr.interrupt()
            textview.text = "Logging..."
            togglebtn.isEnabled = false
            isAmbientMode = true
        }

        override fun onExitAmbient() {
            Log.d("MainActivity", "onExitAmbient")
            togglebtn.isEnabled = true
//            mainActivity.heartrateViewThreadPtr = mainActivity.newHeartrateViewThread()
//            mainActivity.heartrateViewThreadPtr.start()
            isAmbientMode = false
        }

        override fun onUpdateAmbient() {

        }
    }
}