package kr.ac.hallym.healthlogger.statuses

import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.ValueKey
import kr.ac.hallym.healthlogger.listeners.HeartrateListener

enum class HeartrateStatus (val value: Int) {
    TOO_WEAK_PPG(-10),
    WEEK_PPG(-8),
    WEARABLE_DETACHED(-3),
    INITIAL_STATE(0),
    SUCCESSFUL(1);

    companion object {
        fun from(value: Int) = HeartrateStatus.values().first { it.value == value }
        fun from(data: DataPoint) = from(data.getValue(ValueKey.HeartRateSet.STATUS))
    }
}