package kr.ac.hallym.healthlogger.statuses

import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey

enum class SPO2Status(val value: Int) {
    LOW_SIGNAL(-5),
    DEVICE_MOVING(-4),
    INITIAL_STATUS(-1),
    CALCULATING(0),
    MEASUREMENT_COMPLETED(2);

    companion object {
        fun from(value: Int) = SPO2Status.values().first { it.value == value }
        fun from(data: DataPoint) = from(data.getValue(ValueKey.SpO2Set.STATUS))
    }
}