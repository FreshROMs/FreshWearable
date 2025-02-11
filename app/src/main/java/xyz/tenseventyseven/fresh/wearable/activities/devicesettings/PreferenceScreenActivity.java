package xyz.tenseventyseven.fresh.wearable.activities.devicesettings;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import dev.oneuiproject.oneui.layout.ToolbarLayout;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.common.AbstractNoActionBarActivity;
import xyz.tenseventyseven.fresh.databinding.WearActivityDevicePreferenceScreenBinding;
import xyz.tenseventyseven.fresh.wearable.components.preferences.PreferenceList;
import xyz.tenseventyseven.fresh.wearable.interfaces.DeviceSetting;

public class PreferenceScreenActivity extends AbstractNoActionBarActivity {
    WearActivityDevicePreferenceScreenBinding binding;
    private ToolbarLayout toolbarLayout;
    private GBDevice device;
    private DeviceSetting setting;
    private boolean isSwitchBar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = WearActivityDevicePreferenceScreenBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            device = extras.getParcelable(GBDevice.EXTRA_DEVICE);
            setting = extras.getParcelable(DeviceSetting.EXTRA_SETTING);
            isSwitchBar = extras.getBoolean(DeviceSetting.EXTRA_IS_SWITCH_BAR);
        }

        if (device == null) {
            finish();
            return;
        }

        setupToolbar();
        setupSettings();
    }

    private void setupToolbar() {
        toolbarLayout = binding.toolbarLayout;
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbarLayout.setTitle(getString(setting.title));
        if (isSwitchBar) {
            toolbarLayout.getSwitchBar().setVisibility(View.VISIBLE);
            toolbarLayout.getSwitchBar().setChecked(Application.getDevicePrefs(device).getPreferences().getBoolean(setting.key, Boolean.parseBoolean(setting.defaultValue)));
            toolbarLayout.getSwitchBar().addOnSwitchChangeListener(this::onSwitchBarChange);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("ApplySharedPref") // This runs in a thread so it doesn't block the UI
    private void onSwitchBarChange(View v, boolean isChecked) {
        if (!isSwitchBar) return;

        SharedPreferences prefs = Application.getDevicePrefs(device).getPreferences();
        new Thread(() -> {
            prefs.edit().putBoolean(setting.key, isChecked).commit();
            Application.deviceService(device).onSendConfiguration(setting.key);
            device.getDeviceCoordinator().getDeviceSettings(device).onSettingChanged(device, Application.getDevicePrefs(device).getPreferences(), setting.key);
        }).start();
    }

    private void setupSettings() {
        if (device == null) return;
        PreferenceList preferenceList = binding.preferenceList;

        String summary = "";
        if (setting.screenSummary != 0) {
            summary = getString(setting.screenSummary);
        }

        preferenceList.setSettings(this, device, setting.settings, summary);
    }
}