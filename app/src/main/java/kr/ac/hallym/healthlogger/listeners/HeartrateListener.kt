package kr.ac.hallym.healthlogger.listeners

import android.util.Log
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey
import kr.ac.hallym.healthlogger.statuses.HeartrateStatus


// deprecated
object HeartrateListener: HealthTracker.TrackerEventListener, ISensorListener {
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
    }

    override fun onFlushCompleted() {
    }

    override fun onError(p0: HealthTracker.TrackerError?) {
        Log.d("HR", p0.toString())
    }
}