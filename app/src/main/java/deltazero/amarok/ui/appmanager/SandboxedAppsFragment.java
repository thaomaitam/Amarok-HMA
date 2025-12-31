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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import deltazero.amarok.R;
import deltazero.amarok.config.HideConfig;
import deltazero.amarok.utils.XHidePrefBridge;

/**
 * Fragment for managing sandboxed apps (whitelist mode).
 * Apps in this list can only see apps that are explicitly linked.
 */
public class SandboxedAppsFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyState;
    private ExtendedFloatingActionButton fab;
    private SandboxedAppsAdapter adapter;
    private HideConfig config;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sandboxed_apps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.fragment_recycler);
        emptyState = view.findViewById(R.id.fragment_empty_state);
        fab = view.findViewById(R.id.fragment_fab);

        config = HideConfig.getInstance(requireContext());

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SandboxedAppsAdapter();
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
        Set<String> whitelist = config.getWhitelistApps();
        List<SandboxedAppInfo> apps = new ArrayList<>();

        PackageManager pm = requireContext().getPackageManager();
        for (String packageName : whitelist) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                Set<String> linked = config.getLinkedApps(packageName);
                apps.add(new SandboxedAppInfo(
                        packageName,
                        pm.getApplicationLabel(appInfo).toString(),
                        pm.getApplicationIcon(appInfo),
                        linked.size()
                ));
            } catch (PackageManager.NameNotFoundException e) {
                config.removeFromWhitelist(packageName);
            }
        }

        adapter.setApps(apps);
        emptyState.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(apps.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showAppPicker() {
        AppPickerDialog.show(requireContext(), config.getWhitelistApps(), selectedApps -> {
            for (String pkg : selectedApps) {
                if (!config.isWhitelisted(pkg)) {
                    config.addToWhitelist(pkg);
                }
            }
            for (String pkg : config.getWhitelistApps()) {
                if (!selectedApps.contains(pkg)) {
                    config.removeFromWhitelist(pkg);
                }
            }
            XHidePrefBridge.syncConfig(requireContext());
            refreshList();
        });
    }

    private void showLinkEditor(String sandboxedApp) {
        Set<String> currentLinks = config.getLinkedApps(sandboxedApp);
        AppPickerDialog.show(requireContext(), currentLinks, selectedApps -> {
            config.setLinkedApps(sandboxedApp, selectedApps);
            XHidePrefBridge.syncConfig(requireContext());
            refreshList();
        });
    }

    private void removeApp(String packageName) {
        config.removeFromWhitelist(packageName);
        XHidePrefBridge.syncConfig(requireContext());
        refreshList();
    }

    private static class SandboxedAppInfo {
        String packageName;
        String appName;
        Drawable icon;
        int linkedCount;

        SandboxedAppInfo(String packageName, String appName, Drawable icon, int linkedCount) {
            this.packageName = packageName;
            this.appName = appName;
            this.icon = icon;
            this.linkedCount = linkedCount;
        }
    }

    private class SandboxedAppsAdapter extends RecyclerView.Adapter<SandboxedAppsAdapter.ViewHolder> {
        private List<SandboxedAppInfo> apps = new ArrayList<>();

        void setApps(List<SandboxedAppInfo> apps) {
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
            SandboxedAppInfo app = apps.get(position);
            holder.icon.setImageDrawable(app.icon);
            holder.name.setText(app.appName);
            holder.packageName.setText(app.packageName);
            holder.linkedInfo.setVisibility(View.VISIBLE);
            holder.linkedInfo.setText(getString(R.string.linked_apps_count, app.linkedCount));
            
            // Tap item to edit links
            holder.itemView.setOnClickListener(v -> showLinkEditor(app.packageName));
            
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
