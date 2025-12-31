package deltazero.amarok.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.R;
import deltazero.amarok.config.HideConfig;
import deltazero.amarok.ui.appmanager.HiddenAppsFragment;
import deltazero.amarok.ui.appmanager.LauncherFragment;
import deltazero.amarok.ui.appmanager.SandboxedAppsFragment;
import deltazero.amarok.utils.XHidePrefBridge;

/**
 * New App Manager with 3 tabs:
 * - Hidden: Apps hidden from all other apps (blacklist)
 * - Sandboxed: Apps that can only see linked apps (whitelist)
 * - Launcher: Launch hidden apps
 */
public class AppManagerActivity extends AmarokActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MaterialToolbar toolbar;
    private View statusCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_manager);

        toolbar = findViewById(R.id.app_manager_toolbar);
        tabLayout = findViewById(R.id.app_manager_tabs);
        viewPager = findViewById(R.id.app_manager_viewpager);
        statusCard = findViewById(R.id.app_manager_status);

        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup ViewPager with adapter
        viewPager.setAdapter(new AppManagerPagerAdapter(this));

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0 -> tab.setText(R.string.tab_hidden);
                case 1 -> tab.setText(R.string.tab_sandboxed);
                case 2 -> tab.setText(R.string.tab_launcher);
            }
        }).attach();

        // Setup module status card
        setupStatusCard();
    }

    private void setupStatusCard() {
        ImageView statusIcon = statusCard.findViewById(R.id.status_icon);
        TextView statusTitle = statusCard.findViewById(R.id.status_title);
        TextView statusSubtitle = statusCard.findViewById(R.id.status_subtitle);
        Chip statusChip = statusCard.findViewById(R.id.status_chip);

        boolean isActive = XHidePrefBridge.isModuleActive;
        int xposedVersion = XHidePrefBridge.xposedVersion;

        HideConfig config = HideConfig.getInstance(this);
        int hiddenCount = config.getBlacklistApps().size();
        int sandboxedCount = config.getWhitelistApps().size();

        if (isActive) {
            statusIcon.setImageResource(R.drawable.ic_check);
            statusIcon.setImageTintList(getColorStateList(com.google.android.material.R.color.m3_sys_color_dynamic_light_primary));
            statusSubtitle.setText(getString(R.string.module_active, xposedVersion));
            statusChip.setText(getString(R.string.hidden_count, hiddenCount) + " | " + getString(R.string.sandboxed_count, sandboxedCount));
            statusChip.setChipBackgroundColorResource(com.google.android.material.R.color.m3_sys_color_dynamic_light_primary_container);
        } else {
            statusIcon.setImageResource(R.drawable.ic_close);
            statusIcon.setImageTintList(getColorStateList(com.google.android.material.R.color.m3_sys_color_dynamic_light_error));
            statusSubtitle.setText(R.string.module_inactive);
            statusChip.setText(R.string.inactive);
            statusChip.setChipBackgroundColorResource(com.google.android.material.R.color.m3_sys_color_dynamic_light_error_container);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupStatusCard();
    }

    private static class AppManagerPagerAdapter extends FragmentStateAdapter {

        public AppManagerPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 0 -> new HiddenAppsFragment();
                case 1 -> new SandboxedAppsFragment();
                case 2 -> new LauncherFragment();
                default -> throw new IllegalArgumentException("Invalid position: " + position);
            };
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
