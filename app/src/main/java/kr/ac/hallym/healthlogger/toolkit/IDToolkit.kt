package kr.ac.hallym.healthlogger.toolkit

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.util.Optional
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

object IDToolkit {
    fun getID(filesDirPath: String): String {
        val idFile = File(filesDirPath, "id.txt")
        var idCode = if (idFile.exists()) idFile.readText() else ""
        if (idCode == "" || idCode.length != 4) {
            idCode = (abs(Random.nextInt()) % 10000).toString()
            idFile.writeText(idCode)
        }
        return idCode
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getUUID(filesDirPath: String): UUID {
        val uuidFile = File(filesDirPath, "uuid.txt")
        return Optional
            .ofNullable(if (uuidFile.exists()) uuidFile.readText() else null)
            .map(UUID::fromString)
            .orElseGet {
                val uuid = UUID.randomUUID()
                uuidFile.writeText(uuid.toString())
                uuid
            }
    }
}