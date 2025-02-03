package xyz.tenseventyseven.fresh.wearable.activities.devicesettings;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import dev.oneuiproject.oneui.layout.ToolbarLayout;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.common.AbstractNoActionBarActivity;
import xyz.tenseventyseven.fresh.databinding.WearActivityDevicePreferenceScreenBinding;
import xyz.tenseventyseven.fresh.wearable.adapters.DeviceSettingsAdapter;
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

        toolbarLayout.getSwitchBar().setVisibility(isSwitchBar ? View.VISIBLE : View.GONE);
    }

    private void setupSettings() {
        if (device == null) return;

        TextView summary = binding.preferenceScreenSummary;
        ListView listView = binding.deviceSubSettings;

        if (setting.screenSummary != 0) {
            summary.setText(setting.screenSummary);
        } else {
            summary.setVisibility(View.GONE);
        }

        DeviceSettingsAdapter adapter = new DeviceSettingsAdapter(this, device, setting.settings);
        listView.setAdapter(adapter);
    }
}