package deltazero.amarok.xposed.utils

import com.github.kyuubiran.ezxhelper.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.robv.android.xposed.XSharedPreferences
import deltazero.amarok.BuildConfig

/**
 * Xposed-side preference reader for hide configuration.
 * Supports blacklist (hide from all) and whitelist (sandbox) modes.
 */
object XPref {
    private var xPref: XSharedPreferences? = null
    private val gson = Gson()

    const val XPREF_PATH = "deltazero.amarok.hide_config"
    const val KEY_BLACKLIST = "blacklist_apps"
    const val KEY_WHITELIST = "whitelist_apps"
    const val KEY_LINKED = "linked_apps"

    // Cached values
    private var blacklistCache: Set<String> = emptySet()
    private var whitelistCache: Set<String> = emptySet()
    private var linkedCache: Map<String, Set<String>> = emptyMap()

    fun init() {
        Log.d("Initializing XPref...", null)

        xPref = XSharedPreferences(BuildConfig.APPLICATION_ID, XPREF_PATH)
        Log.d("xPref path: ${xPref?.file?.absolutePath}", null)

        xPref?.let {
            if (it.file.canRead()) {
                it.reload()
                refreshCache()
                Log.d("xPref loaded successfully", null)
            } else {
                Log.wx("No XPref found. Launch Amarok once to configure.", null)
            }
        }

        Log.ix("XPref initialized.", null)
    }

    fun refreshCache() {
        xPref?.let { pref ->
            pref.reload()
            if (!pref.file.canRead()) return

            blacklistCache = pref.getStringSet(KEY_BLACKLIST, emptySet()) ?: emptySet()
            whitelistCache = pref.getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet()

            val linkedJson = pref.getString(KEY_LINKED, "{}") ?: "{}"
            linkedCache = try {
                val type = object : TypeToken<Map<String, Set<String>>>() {}.type
                gson.fromJson(linkedJson, type) ?: emptyMap()
            } catch (e: Exception) {
                Log.ex("Failed to parse linked apps JSON", e)
                emptyMap()
            }

            Log.d("Cache refreshed: blacklist=${blacklistCache.size}, whitelist=${whitelistCache.size}", null)
        }
    }

    /**
     * Determine if target should be hidden from caller.
     * 
     * Logic:
     * 1. If target is blacklisted → HIDE from everyone
     * 2. If caller is whitelisted (sandboxed) → HIDE unless target is linked
     */
    fun shouldHide(caller: String?, target: String?): Boolean {
        if (caller == null && target == null) return false
        if (caller == target) return false

        // Rule 1: Blacklist - target hidden from ALL
        if (target != null && target in blacklistCache) {
            return true
        }

        // Rule 2: Whitelist - caller is sandboxed
        if (caller != null && caller in whitelistCache) {
            val allowedApps = linkedCache[caller]
            return target !in (allowedApps ?: emptySet())
        }

        return false
    }

    fun isBlacklisted(packageName: String): Boolean = packageName in blacklistCache
    
    fun isWhitelisted(packageName: String): Boolean = packageName in whitelistCache

    fun getXPrefDir(): String = XSharedPreferences(BuildConfig.APPLICATION_ID, XPREF_PATH)
        .file.parentFile?.absolutePath ?: ""
}
