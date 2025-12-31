package deltazero.amarok.xposed.hooks

import android.os.Build
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import deltazero.amarok.xposed.utils.Utils
import deltazero.amarok.xposed.utils.XPref
import deltazero.amarok.xposed.logD
import deltazero.amarok.xposed.logI
import deltazero.amarok.xposed.logE
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "PmsHookTarget30"
private const val UID_SYSTEM = 1000

/**
 * HMA-style hook for Android 11-12 (API 30-32).
 * Hooks AppsFilter.shouldFilterApplication
 */
@RequiresApi(Build.VERSION_CODES.R)
class PmsHookTarget30 : IFrameworkHook {

    private var hook: XC_MethodHook.Unhook? = null
    private var lastFilteredApp: AtomicReference<String?> = AtomicReference(null)

    override fun load() {
        logI(TAG, "Load hook")
        hook = findMethod("com.android.server.pm.AppsFilter") {
            name == "shouldFilterApplication"
        }.hookBefore { param ->
            runCatching {
                XPref.refreshCache()
                
                val callingUid = param.args[0] as Int
                if (callingUid == UID_SYSTEM) return@hookBefore
                
                val targetApp = Utils.getPackageNameFromPackageSettings(param.args[2])
                    ?: return@hookBefore
                
                // For API 30-32, check if target should be hidden
                if (XPref.shouldHide(null, targetApp)) {
                    param.result = true
                    val last = lastFilteredApp.getAndSet("uid:$callingUid")
                    if (last != "uid:$callingUid") logI(TAG, "@shouldFilterApplication: query from uid:$callingUid")
                    logD(TAG, "@shouldFilterApplication caller: $callingUid, target: $targetApp")
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unload()
            }
        }
    }

    override fun unload() {
        hook?.unhook()
        hook = null
    }
}
