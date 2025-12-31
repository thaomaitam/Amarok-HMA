package deltazero.amarok.ui.settings;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import deltazero.amarok.R;
import deltazero.amarok.config.HideConfig;
import deltazero.amarok.ui.AppManagerActivity;
import deltazero.amarok.utils.XHidePrefBridge;

/**
 * XHide settings category.
 * Now uses always-on protection model - no toggle needed.
 */
public class XHideCategory extends BaseCategory {
    public XHideCategory(@NonNull FragmentActivity activity, @NonNull PreferenceScreen screen) {
        super(activity, screen);
        setTitle(R.string.x_hide);

        // Module status
        var moduleStatus = new Preference(activity);
        moduleStatus.setTitle(R.string.module_status);
        moduleStatus.setIcon(R.drawable.domino_mask_fill0_wght400_grad0_opsz24);
        if (XHidePrefBridge.isModuleActive) {
            moduleStatus.setSummary(activity.getString(R.string.module_active, XHidePrefBridge.xposedVersion));
        } else {
            moduleStatus.setSummary(R.string.module_inactive);
        }
        addPreference(moduleStatus);

        // Protection summary
        HideConfig config = HideConfig.getInstance(activity);
        int hiddenCount = config.getBlacklistApps().size();
        int sandboxedCount = config.getWhitelistApps().size();

        var protectionStatus = new Preference(activity);
        protectionStatus.setTitle(R.string.protection_active);
        protectionStatus.setSummary(activity.getString(R.string.hidden_count, hiddenCount) + 
                " | " + activity.getString(R.string.sandboxed_count, sandboxedCount));
        protectionStatus.setIcon(R.drawable.ic_lock);
        addPreference(protectionStatus);

        // Open App Manager
        var openAppManager = new Preference(activity);
        openAppManager.setTitle(R.string.app_manager_title);
        openAppManager.setSummary(R.string.x_hide_description);
        openAppManager.setIcon(R.drawable.ic_app);
        openAppManager.setOnPreferenceClickListener(p -> {
            activity.startActivity(new Intent(activity, AppManagerActivity.class));
            return true;
        });
        addPreference(openAppManager);
    }
}

