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

private const val TAG = "PmsHookTarget33"
private const val UID_SYSTEM = 1000

/**
 * HMA-style hook for Android 13 (API 33).
 * Hooks AppsFilterImpl.shouldFilterApplication
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class PmsHookTarget33 : IFrameworkHook {

    private val getPackagesForUidMethod by lazy {
        findMethod("com.android.server.pm.Computer") {
            name == "getPackagesForUid"
        }
    }

    private var hook: XC_MethodHook.Unhook? = null
    private var lastFilteredApp: AtomicReference<String?> = AtomicReference(null)

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG, "Load hook")
        hook = findMethod("com.android.server.pm.AppsFilterImpl", findSuper = true) {
            name == "shouldFilterApplication"
        }.hookBefore { param ->
            runCatching {
                XPref.refreshCache()
                
                val snapshot = param.args[0]
                val callingUid = param.args[1] as Int
                if (callingUid == UID_SYSTEM) return@hookBefore
                
                val callingApps = Utils.binderLocalScope {
                    getPackagesForUidMethod.invoke(snapshot, callingUid) as Array<String>?
                } ?: return@hookBefore
                
                val targetApp = Utils.getPackageNameFromPackageSettings(param.args[3])
                    ?: return@hookBefore
                
                for (caller in callingApps) {
                    if (XPref.shouldHide(caller, targetApp)) {
                        param.result = true
                        val last = lastFilteredApp.getAndSet(caller)
                        if (last != caller) logI(TAG, "@shouldFilterApplication: query from $caller")
                        logD(TAG, "@shouldFilterApplication caller: $callingUid $caller, target: $targetApp")
                        return@hookBefore
                    }
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
