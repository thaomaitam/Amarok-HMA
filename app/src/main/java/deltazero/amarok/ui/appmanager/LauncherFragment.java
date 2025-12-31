package deltazero.amarok.ui.appmanager;

import android.content.Intent;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import deltazero.amarok.R;
import deltazero.amarok.config.HideConfig;

/**
 * Fragment for launching hidden apps.
 * Hidden apps don't appear in the launcher, so they can be launched from here.
 */
public class LauncherFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyState;
    private LauncherAdapter adapter;
    private HideConfig config;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_launcher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.fragment_recycler);
        emptyState = view.findViewById(R.id.fragment_empty_state);

        config = HideConfig.getInstance(requireContext());

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LauncherAdapter();
        recyclerView.setAdapter(adapter);

        refreshList();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        Set<String> hiddenApps = config.getAllHiddenApps();
        List<AppInfo> apps = new ArrayList<>();

        PackageManager pm = requireContext().getPackageManager();
        for (String packageName : hiddenApps) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                apps.add(new AppInfo(
                        packageName,
                        pm.getApplicationLabel(appInfo).toString(),
                        pm.getApplicationIcon(appInfo)
                ));
            } catch (PackageManager.NameNotFoundException e) {
                // App not installed, skip
            }
        }

        adapter.setApps(apps);
        emptyState.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(apps.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void launchApp(String packageName) {
        PackageManager pm = requireContext().getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(requireContext(), "Cannot launch app", Toast.LENGTH_SHORT).show();
        }
    }

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

    private class LauncherAdapter extends RecyclerView.Adapter<LauncherAdapter.ViewHolder> {
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
            
            // Launch icon
            holder.actionButton.setImageResource(R.drawable.ic_open);
            holder.actionButton.setOnClickListener(v -> launchApp(app.packageName));
            
            // Tap item to launch
            holder.itemView.setOnClickListener(v -> launchApp(app.packageName));
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
