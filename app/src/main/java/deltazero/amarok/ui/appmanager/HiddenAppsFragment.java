package deltazero.amarok.ui.appmanager;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import deltazero.amarok.R;
import deltazero.amarok.config.HideConfig;
import deltazero.amarok.utils.XHidePrefBridge;

/**
 * Fragment for managing hidden apps (blacklist mode).
 * Apps in this list are invisible to all other apps.
 */
public class HiddenAppsFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyState;
    private ExtendedFloatingActionButton fab;
    private HiddenAppsAdapter adapter;
    private HideConfig config;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hidden_apps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.fragment_recycler);
        emptyState = view.findViewById(R.id.fragment_empty_state);
        fab = view.findViewById(R.id.fragment_fab);

        config = HideConfig.getInstance(requireContext());

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HiddenAppsAdapter();
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> showAppPicker());

        refreshList();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        Set<String> blacklist = config.getBlacklistApps();
        List<AppInfo> apps = new ArrayList<>();

        PackageManager pm = requireContext().getPackageManager();
        for (String packageName : blacklist) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                apps.add(new AppInfo(
                        packageName,
                        pm.getApplicationLabel(appInfo).toString(),
                        pm.getApplicationIcon(appInfo)
                ));
            } catch (PackageManager.NameNotFoundException e) {
                // App uninstalled, remove from list
                config.removeFromBlacklist(packageName);
            }
        }

        adapter.setApps(apps);
        emptyState.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(apps.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showAppPicker() {
        AppPickerDialog.show(requireContext(), config.getBlacklistApps(), selectedApps -> {
            // Add new apps to blacklist
            for (String pkg : selectedApps) {
                if (!config.isBlacklisted(pkg)) {
                    config.addToBlacklist(pkg);
                }
            }
            // Remove unselected apps
            for (String pkg : config.getBlacklistApps()) {
                if (!selectedApps.contains(pkg)) {
                    config.removeFromBlacklist(pkg);
                }
            }
            XHidePrefBridge.syncConfig(requireContext());
            refreshList();
        });
    }

    private void removeApp(String packageName) {
        config.removeFromBlacklist(packageName);
        XHidePrefBridge.syncConfig(requireContext());
        refreshList();
    }

    // Data class for app info
    private static class AppInfo {
        String packageName;
        String appName;
        Drawable icon;

        AppInfo(String packageName, String appName, Drawable icon) {
            this.packageName = packageName;
            this.appName = appName;
            this.icon = icon;
        }
    }

    // RecyclerView Adapter
    private class HiddenAppsAdapter extends RecyclerView.Adapter<HiddenAppsAdapter.ViewHolder> {
        private List<AppInfo> apps = new ArrayList<>();

        void setApps(List<AppInfo> apps) {
            this.apps = apps;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app_manager, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppInfo app = apps.get(position);
            holder.icon.setImageDrawable(app.icon);
            holder.name.setText(app.appName);
            holder.packageName.setText(app.packageName);
            holder.linkedInfo.setVisibility(View.GONE);
            holder.actionButton.setImageResource(R.drawable.ic_close);
            holder.actionButton.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.remove)
                        .setMessage(getString(R.string.remove_hide_path_description, app.appName))
                        .setPositiveButton(R.string.confirm, (d, w) -> removeApp(app.packageName))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name, packageName, linkedInfo;
            ImageButton actionButton;

            ViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.item_app_icon);
                name = itemView.findViewById(R.id.item_app_name);
                packageName = itemView.findViewById(R.id.item_app_package);
                linkedInfo = itemView.findViewById(R.id.item_app_linked);
                actionButton = itemView.findViewById(R.id.item_app_action);
            }
        }
    }
}
