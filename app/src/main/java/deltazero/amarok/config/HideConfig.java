package deltazero.amarok.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for app hiding with blacklist and whitelist modes.
 * 
 * - Blacklist: Apps that are hidden from ALL other apps (e.g., Magisk, LSPosed)
 * - Whitelist: Apps that are sandboxed and can only see linked apps
 */
public class HideConfig {

    private static final String PREF_NAME = "deltazero.amarok.hide_config";
    private static final String KEY_BLACKLIST = "blacklist_apps";
    private static final String KEY_WHITELIST = "whitelist_apps";
    private static final String KEY_LINKED = "linked_apps";

    private static HideConfig instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    // Blacklist: Apps hidden from ALL other apps
    private Set<String> blacklistApps;

    // Whitelist: Apps that are sandboxed (can't see other apps)
    private Set<String> whitelistApps;

    // Linked apps: For each whitelisted app, which apps it CAN see
    private Map<String, Set<String>> linkedApps;

    private HideConfig(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        load();
    }

    public static synchronized HideConfig getInstance(Context context) {
        if (instance == null) {
            instance = new HideConfig(context.getApplicationContext());
        }
        return instance;
    }

    private void load() {
        // Load blacklist
        blacklistApps = prefs.getStringSet(KEY_BLACKLIST, new HashSet<>());
        blacklistApps = new HashSet<>(blacklistApps); // Make mutable copy

        // Load whitelist
        whitelistApps = prefs.getStringSet(KEY_WHITELIST, new HashSet<>());
        whitelistApps = new HashSet<>(whitelistApps);

        // Load linked apps (stored as JSON)
        String linkedJson = prefs.getString(KEY_LINKED, "{}");
        Type type = new TypeToken<Map<String, Set<String>>>() {}.getType();
        linkedApps = gson.fromJson(linkedJson, type);
        if (linkedApps == null) {
            linkedApps = new HashMap<>();
        }
    }

    public void save() {
        prefs.edit()
                .putStringSet(KEY_BLACKLIST, blacklistApps)
                .putStringSet(KEY_WHITELIST, whitelistApps)
                .putString(KEY_LINKED, gson.toJson(linkedApps))
                .apply();
    }

    // ==================== Blacklist Methods ====================

    public Set<String> getBlacklistApps() {
        return new HashSet<>(blacklistApps);
    }

    public void addToBlacklist(String packageName) {
        blacklistApps.add(packageName);
        save();
    }

    public void removeFromBlacklist(String packageName) {
        blacklistApps.remove(packageName);
        save();
    }

    public boolean isBlacklisted(String packageName) {
        return blacklistApps.contains(packageName);
    }

    // ==================== Whitelist Methods ====================

    public Set<String> getWhitelistApps() {
        return new HashSet<>(whitelistApps);
    }

    public void addToWhitelist(String packageName) {
        whitelistApps.add(packageName);
        if (!linkedApps.containsKey(packageName)) {
            linkedApps.put(packageName, new HashSet<>());
        }
        save();
    }

    public void removeFromWhitelist(String packageName) {
        whitelistApps.remove(packageName);
        linkedApps.remove(packageName);
        save();
    }

    public boolean isWhitelisted(String packageName) {
        return whitelistApps.contains(packageName);
    }

    // ==================== Linked Apps Methods ====================

    public Set<String> getLinkedApps(String whitelistedApp) {
        Set<String> linked = linkedApps.get(whitelistedApp);
        return linked != null ? new HashSet<>(linked) : new HashSet<>();
    }

    public void setLinkedApps(String whitelistedApp, Set<String> apps) {
        linkedApps.put(whitelistedApp, new HashSet<>(apps));
        save();
    }

    public void addLinkedApp(String whitelistedApp, String linkedApp) {
        Set<String> linked = linkedApps.computeIfAbsent(whitelistedApp, k -> new HashSet<>());
        linked.add(linkedApp);
        save();
    }

    public void removeLinkedApp(String whitelistedApp, String linkedApp) {
        Set<String> linked = linkedApps.get(whitelistedApp);
        if (linked != null) {
            linked.remove(linkedApp);
            save();
        }
    }

    // ==================== Export for Xposed ====================

    /**
     * Export configuration as JSON for Xposed module to read.
     * This is used by XHidePrefBridge to sync config to Xposed.
     */
    public String exportAsJson() {
        Map<String, Object> config = new HashMap<>();
        config.put("blacklist", blacklistApps);
        config.put("whitelist", whitelistApps);
        config.put("linked", linkedApps);
        return gson.toJson(config);
    }

    /**
     * Export linked apps as JSON string for XPref.
     */
    public String exportLinkedAppsJson() {
        return gson.toJson(linkedApps);
    }

    /**
     * Get all hidden apps (for launcher feature)
     */
    public Set<String> getAllHiddenApps() {
        return getBlacklistApps();
    }
}
