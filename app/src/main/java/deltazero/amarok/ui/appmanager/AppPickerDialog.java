package deltazero.amarok.ui.appmanager;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import deltazero.amarok.R;

/**
 * Dialog for picking apps from installed apps list with search.
 */
public class AppPickerDialog {

    public interface OnAppsSelectedListener {
        void onAppsSelected(Set<String> selectedPackages);
    }

    public static void show(Context context, Set<String> preSelected, OnAppsSelectedListener listener) {
        new Thread(() -> {
            List<AppInfo> apps = loadInstalledApps(context);
            ((android.app.Activity) context).runOnUiThread(() -> 
                showDialog(context, apps, preSelected, listener));
        }).start();
    }

    private static List<AppInfo> loadInstalledApps(Context context) {
        List<AppInfo> apps = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        for (ApplicationInfo appInfo : installedApps) {
            // Skip self
            if (appInfo.packageName.equals(context.getPackageName())) continue;
            
            boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            
            apps.add(new AppInfo(
                    appInfo.packageName,
                    pm.getApplicationLabel(appInfo).toString(),
                    pm.getApplicationIcon(appInfo),
                    isSystem
            ));
        }
        
        // Sort by name
        apps.sort((a, b) -> a.appName.compareToIgnoreCase(b.appName));
        
        return apps;
    }

    private static void showDialog(Context context, List<AppInfo> allApps, Set<String> preSelected, OnAppsSelectedListener listener) {
        Set<String> selected = new HashSet<>(preSelected);
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_app_picker, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.dialog_recycler);
        EditText searchBox = dialogView.findViewById(R.id.dialog_search);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        AppPickerAdapter adapter = new AppPickerAdapter(allApps, selected);
        recyclerView.setAdapter(adapter);
        
        // Search filter
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().toLowerCase(Locale.getDefault());
                List<AppInfo> filtered = new ArrayList<>();
                for (AppInfo app : allApps) {
                    if (app.appName.toLowerCase(Locale.getDefault()).contains(query) ||
                        app.packageName.toLowerCase(Locale.getDefault()).contains(query)) {
                        filtered.add(app);
                    }
                }
                adapter.setApps(filtered);
            }
        });
        
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.select_apps)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (dialog, which) -> listener.onAppsSelected(selected))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static class AppInfo {
        String packageName;
        String appName;
        Drawable icon;
        boolean isSystem;

        AppInfo(String packageName, String appName, Drawable icon, boolean isSystem) {
            this.packageName = packageName;
            this.appName = appName;
            this.icon = icon;
            this.isSystem = isSystem;
        }
    }

    private static class AppPickerAdapter extends RecyclerView.Adapter<AppPickerAdapter.ViewHolder> {
        private List<AppInfo> apps;
        private final Set<String> selected;

        AppPickerAdapter(List<AppInfo> apps, Set<String> selected) {
            this.apps = new ArrayList<>(apps);
            this.selected = selected;
        }
        
        void setApps(List<AppInfo> apps) {
            this.apps = new ArrayList<>(apps);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app_picker, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppInfo app = apps.get(position);
            holder.icon.setImageDrawable(app.icon);
            holder.name.setText(app.appName);
            holder.packageName.setText(app.packageName + (app.isSystem ? " (System)" : ""));
            holder.checkbox.setOnCheckedChangeListener(null);
            holder.checkbox.setChecked(selected.contains(app.packageName));
            
            View.OnClickListener clickListener = v -> {
                holder.checkbox.setChecked(!holder.checkbox.isChecked());
            };
            holder.itemView.setOnClickListener(clickListener);
            
            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selected.add(app.packageName);
                } else {
                    selected.remove(app.packageName);
                }
            });
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name, packageName;
            CheckBox checkbox;

            ViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.item_app_icon);
                name = itemView.findViewById(R.id.item_app_name);
                packageName = itemView.findViewById(R.id.item_app_package);
                checkbox = itemView.findViewById(R.id.item_app_checkbox);
            }
        }
    }
}
