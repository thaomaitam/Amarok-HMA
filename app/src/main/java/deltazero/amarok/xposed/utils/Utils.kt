package deltazero.amarok.xposed.utils

import android.os.Binder
import android.os.Build
import com.github.kyuubiran.ezxhelper.utils.findField

object Utils {

    fun <T> binderLocalScope(block: () -> T): T {
        val identity = Binder.clearCallingIdentity()
        val result = block()
        Binder.restoreCallingIdentity(identity)
        return result
    }

    fun getPackageNameFromPackageSettings(packageSettings: Any): String? {
        return runCatching {
            findField(packageSettings::class.java, true) {
                name == if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "mName" else "name"
            }.get(packageSettings) as? String
        }.getOrNull()
    }
}
