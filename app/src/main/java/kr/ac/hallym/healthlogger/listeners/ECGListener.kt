package kr.ac.hallym.healthlogger.listeners

import android.util.Log
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey


// adb shell logcat --pid=`adb shell ps | grep kr.ac.hallym | awk '{print $2}'` | awk '/ECGListener/{print $NF}'
object ECGListener : HealthTracker.TrackerEventListener {
    override fun onDataReceived(p0: MutableList<DataPoint>) {
        val line = p0.first().timestamp.toString() + "," +
                p0.joinToString(",") { "${it.getValue(ValueKey.EcgSet.ECG)}" }
        Log.d(javaClass.simpleName, line)
//        p0.forEach {
////            it.getValue(ValueKey.EcgSet.ECG)
//            Log.d("ECGListener", "${it.timestamp},${it.getValue(ValueKey.EcgSet.ECG)}")
//        }
    }

    override fun onFlushCompleted() {
        TODO("Not yet implemented")
    }

    override fun onError(p0: HealthTracker.TrackerError?) {
        Log.d("ListenerError", p0.toString())
    }
}