package kr.ac.hallym.healthlogger

import kr.ac.hallym.healthlogger.services.HealthTrackingAndroidService

enum class Action {
    START_LOGGING,
    END_LOGGING,
    IS_LOGGING,
    IS_NOT_LOGGING,
    DATA_RECEIVED,
    REQ_ALIVE,
    RES_ALIVE,
    REQ_DATA,
    RES_DATA,
    Q_IS_LOGGING,
    SEND_SUCCESSFUL,
    SEND_FAILED;

    fun isFor(cls: Class<Any>) = when (this) {
        START_LOGGING,
        END_LOGGING,
        REQ_ALIVE,
        REQ_DATA,
        Q_IS_LOGGING-> cls == HealthTrackingAndroidService::class.java

        IS_LOGGING,
        IS_NOT_LOGGING,
        DATA_RECEIVED,
        RES_ALIVE,
        RES_DATA -> cls == MainActivity::class.java

        else -> true
    }

    override fun toString(): String {
        return name
    }
}