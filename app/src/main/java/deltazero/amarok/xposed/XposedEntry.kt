package deltazero.amarok.xposed

import android.os.Build
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import deltazero.amarok.BuildConfig
import deltazero.amarok.xposed.hooks.IFrameworkHook
import deltazero.amarok.xposed.hooks.PmsHookTarget30
import deltazero.amarok.xposed.hooks.PmsHookTarget33
import deltazero.amarok.xposed.hooks.PmsHookTarget34
import deltazero.amarok.xposed.utils.XPref

private const val TAG = "Amarok-XHide"

/**
 * Xposed module entry point.
 * Uses HMA-style hooks for comprehensive app hiding.
 */
class XposedEntry : IXposedHookLoadPackage, IXposedHookZygoteInit {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        EzXHelper.initHandleLoadPackage(lpparam)
        when (lpparam.packageName) {
            BuildConfig.APPLICATION_ID -> loadSelfHooks(lpparam)
            "android" -> {
                XPref.init()
                loadSystemHooks()
            }
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelper.initZygote(startupParam)
        EzXHelper.setLogTag(TAG)
        EzXHelper.setToastTag(TAG)
    }

    private fun loadSystemHooks() {
        logI(TAG, "Initializing HMA-style system hooks...")
        
        val hooks = mutableListOf<IFrameworkHook>()

        when {
            Build.VERSION.SDK_INT > 36 -> {
                logE(TAG, "Unsupported Android version (API > 36)")
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                logI(TAG, "Using PmsHookTarget34 for API 34+")
                hooks.add(PmsHookTarget34())
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                logI(TAG, "Using PmsHookTarget33 for API 33")
                hooks.add(PmsHookTarget33())
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                logI(TAG, "Using PmsHookTarget30 for API 30-32")
                hooks.add(PmsHookTarget30())
            }
            else -> {
                logW(TAG, "API ${Build.VERSION.SDK_INT} not supported for HMA-style hooks")
            }
        }

        hooks.forEach { it.load() }
        logI(TAG, "System hooks loaded: ${hooks.size}")
    }

    private fun loadSelfHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        Log.d("Loading self hooks...", null)

        val c = XposedHelpers.findClass("deltazero.amarok.utils.XHidePrefBridge", lpparam.classLoader)
        XposedHelpers.setStaticBooleanField(c, "isModuleActive", true)
        XposedHelpers.setStaticIntField(c, "xposedVersion", XposedBridge.getXposedVersion())
        XposedHelpers.setStaticObjectField(c, "xPrefDir", XPref.getXPrefDir())

        Log.ix("Self hooks loaded.", null)
    }
}
