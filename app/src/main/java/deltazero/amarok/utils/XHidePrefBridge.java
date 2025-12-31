package deltazero.amarok.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import deltazero.amarok.BuildConfig;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.config.HideConfig;
import deltazero.amarok.xposed.utils.XPref;

/**
 * Bridge between Amarok app and Xposed module.
 * Syncs HideConfig (blacklist, whitelist, linked apps) to XSharedPreferences
 * for the Xposed module to read.
 * 
 * Always-on protection: No toggle needed. If module is active, protection is active.
 */
@SuppressLint("SdCardPath")
public class XHidePrefBridge {
    private static final String TAG = "XHidePrefBridge";
    private static final String LOCAL_PREF_DIR = String.format("/data/data/%s/shared_prefs",
            BuildConfig.APPLICATION_ID);
    private static final String HIDE_CONFIG_FILENAME = "deltazero.amarok.hide_config.xml";

    private static SharedPreferences.Editor xprefEditor;

    public static boolean isModuleActive = false; /* Hooked by module */
    public static int xposedVersion = 0; /* Hooked by module */
    public static String xPrefDir = ""; /* Hooked by module */

    public static boolean isAvailable = false;

    /**
     * Migrate preferences when Xposed module is first enabled.
     */
    public static void migratePrefsIfNeeded(Context context) {
        var localPrefFile = new File(LOCAL_PREF_DIR, HIDE_CONFIG_FILENAME);
        var xPrefFile = new File(xPrefDir, HIDE_CONFIG_FILENAME);

        if (!isModuleActive || xPrefFile.exists() || !localPrefFile.exists())
            return;

        Log.w(TAG, String.format("Migrating preferences to XPref directory: %s -> %s",
                LOCAL_PREF_DIR, xPrefDir));
        try {
            Files.copy(localPrefFile.toPath(), xPrefFile.toPath());
            Log.i(TAG, "Preferences migrated successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to migrate preferences", e);
        }
    }

    @SuppressLint("WorldReadableFiles")
    public static void init(Context context) {
        if (!isModuleActive) {
            Log.i(TAG, "Xposed module not active");
            return;
        }

        Log.i(TAG, "Xposed module active, version = " + xposedVersion);

        SharedPreferences xPref;
        try {
            // Use same pref path as XPref on module side
            xPref = context.getSharedPreferences(XPref.XPREF_PATH, Context.MODE_WORLD_READABLE);
        } catch (SecurityException ignored) {
            Log.w(TAG, "Unsupported Xposed framework. XHide unavailable.");
            return;
        }

        xprefEditor = xPref.edit();

        // Sync current config to XPref
        syncConfig(context);

        Log.i(TAG, "XHidePrefBridge initialized. Always-on protection active.");
        isAvailable = true;
    }

    /**
     * Sync HideConfig to XSharedPreferences.
     * Call this whenever blacklist, whitelist, or linked apps change.
     */
    public static void syncConfig(Context context) {
        if (xprefEditor == null) {
            Log.w(TAG, "XPref not initialized, cannot sync");
            return;
        }

        HideConfig config = HideConfig.getInstance(context);

        Log.d(TAG, "Syncing config: blacklist=" + config.getBlacklistApps().size() +
                ", whitelist=" + config.getWhitelistApps().size());

        xprefEditor.putStringSet(XPref.KEY_BLACKLIST, config.getBlacklistApps());
        xprefEditor.putStringSet(XPref.KEY_WHITELIST, config.getWhitelistApps());
        xprefEditor.putString(XPref.KEY_LINKED, config.exportLinkedAppsJson());
        xprefEditor.commit();

        Log.d(TAG, "Config synced to XPref");
    }

    /**
     * Check if module is active and available.
     */
    public static boolean isModuleAvailable() {
        return isModuleActive && isAvailable;
    }
}

