package kr.ac.hallym.healthlogger.listeners

import android.annotation.SuppressLint
import android.util.Log
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import kr.ac.hallym.healthlogger.statuses.SPO2Status


object SPO2Listener : HealthTracker.TrackerEventListener, ISensorListener {
    @SuppressLint("StaticFieldLeak")
    lateinit var healthTrackingService: HealthTrackingService
    private var lastSPO2: Int = 0

    override fun onDataReceived(p0: MutableList<DataPoint>) {
        Log.d("SPO2", "onDataReceived")
        val data = p0.first()
        val status: SPO2Status = SPO2Status.from(data)
        Log.d("SPO2", status.name)
        if (status == SPO2Status.MEASUREMENT_COMPLETED) {
            healthTrackingService
                .getHealthTracker(HealthTrackerType.SPO2)
                .unsetEventListener()
            healthTrackingService
                .getHealthTracker(HealthTrackerType.SPO2)
                .setEventListener(this)
            lastSPO2 = data.getValue(ValueKey.SpO2Set.SPO2)
            Log.d("SPO2", lastSPO2.toString())
        }
    }

    override fun onFlushCompleted() {
    }

    override fun onError(p0: HealthTracker.TrackerError?) {
    }

    fun getValue() = lastSPO2
}