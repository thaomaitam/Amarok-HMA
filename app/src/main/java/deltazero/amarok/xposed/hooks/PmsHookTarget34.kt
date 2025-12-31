package deltazero.amarok.xposed.hooks

import android.os.Binder
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import deltazero.amarok.xposed.utils.Utils
import deltazero.amarok.xposed.utils.XPref
import deltazero.amarok.xposed.logD
import deltazero.amarok.xposed.logI
import deltazero.amarok.xposed.logE
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "PmsHookTarget34"
private const val UID_SYSTEM = 1000

/**
 * HMA-style hook for Android 14+ (API 34+).
 * Hooks AppsFilterImpl.shouldFilterApplication and getArchivedPackageInternal
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PmsHookTarget34 : IFrameworkHook {

    private val getPackagesForUidMethod by lazy {
        findMethod("com.android.server.pm.Computer") {
            name == "getPackagesForUid"
        }
    }

    private var hook: XC_MethodHook.Unhook? = null
    private var exphook: XC_MethodHook.Unhook? = null
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
        
        // AOSP exploit - https://github.com/aosp-mirror/platform_frameworks_base/commit/5bc482bd99ea18fe0b4064d486b29d5ae2d65139
        // Only 14 QPR2+ has this method
        exphook = findMethodOrNull("com.android.server.pm.PackageManagerService", findSuper = true) {
            name == "getArchivedPackageInternal"
        }?.hookBefore { param ->
            runCatching {
                XPref.refreshCache()
                
                val callingUid = Binder.getCallingUid()
                if (callingUid == UID_SYSTEM) return@hookBefore
                
                val targetApp = param.args[0].toString()
                
                // Check if target should be hidden (blacklist check)
                if (XPref.isBlacklisted(targetApp)) {
                    param.result = null
                    val last = lastFilteredApp.getAndSet("uid:$callingUid")
                    if (last != "uid:$callingUid") logI(TAG, "@getArchivedPackageInternal: query from uid:$callingUid")
                    logD(TAG, "@getArchivedPackageInternal caller: $callingUid, target: $targetApp")
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
        exphook?.unhook()
        exphook = null
    }
}
