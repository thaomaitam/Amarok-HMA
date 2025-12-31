package deltazero.amarok.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.apphider.BaseAppHider;
import deltazero.amarok.apphider.NoneAppHider;
import deltazero.amarok.apphider.RootAppHider;

/**
 * Simplified for Root + LSPosed only.
 * Shizuku, Dhizuku, DSM options removed.
 */
public class SwitchAppHiderActivity extends AmarokActivity {

    MaterialToolbar tbToolBar;
    RadioButton rbDisabled, rbRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_apphider);

        rbDisabled = findViewById(R.id.switch_apphider_radio_disabled);
        rbRoot = findViewById(R.id.switch_apphider_radio_root);
        tbToolBar = findViewById(R.id.switch_apphider_tb_toolbar);

        setCheckedRadioButton(PrefMgr.getAppHider(this).getClass());

        tbToolBar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setCheckedRadioButton(PrefMgr.getAppHider(this).getClass());
    }

    public void onCheckAppHiderRadioButton(View view) {
        int buttonID = view.getId();
        if (((RadioButton) view).isChecked()) {
            if (buttonID == R.id.switch_apphider_radio_disabled) {
                new NoneAppHider(this).tryToActivate(this::onActivationCallback);
            } else if (buttonID == R.id.switch_apphider_radio_root) {
                new RootAppHider(this).tryToActivate(this::onActivationCallback);
            }
        }
    }

    public void onClickLearnMoreButton(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.hideapp_doc_url))));
    }

    public void onClickOKButton(View view) {
        finish();
    }

    public void onActivationCallback(Class<? extends BaseAppHider> appHider, boolean success, @Nullable Integer msgResID) {
        if (success) {
            PrefMgr.setAppHiderMode(appHider);
            setCheckedRadioButton(appHider);
        } else {
            assert msgResID != null && msgResID != 0;

            PrefMgr.setAppHiderMode(NoneAppHider.class);
            setCheckedRadioButton(NoneAppHider.class);

            runOnUiThread(() -> new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.apphider_not_ava_title)
                    .setMessage(msgResID)
                    .setPositiveButton(getString(R.string.ok), null)
                    .setNegativeButton(R.string.help, (dialog, which)
                            -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.common_error_doc_url)))))
                    .show());
        }
    }

    private void setCheckedRadioButton(Class<? extends BaseAppHider> appHider) {
        rbDisabled.setChecked(false);
        rbRoot.setChecked(false);

        if (appHider.isAssignableFrom(NoneAppHider.class)) {
            rbDisabled.setChecked(true);
        } else if (appHider.isAssignableFrom(RootAppHider.class)) {
            rbRoot.setChecked(true);
        }
    }
}