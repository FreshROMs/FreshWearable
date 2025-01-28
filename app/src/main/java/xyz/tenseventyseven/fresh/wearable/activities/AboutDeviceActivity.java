package xyz.tenseventyseven.fresh.wearable.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import xyz.tenseventyseven.fresh.common.CommonActivityAbstract;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.databinding.ActivityAboutDeviceBinding;

public class AboutDeviceActivity extends CommonActivityAbstract {
    private static final Logger LOG = LoggerFactory.getLogger(AboutDeviceActivity.class);

    private ActivityAboutDeviceBinding binding;

    private GBDevice mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        LOG.debug("onCreate");
        mDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (mDevice == null) {
            LOG.error("device must not be null");
            finish();
            return;
        }

        LOG.debug("device: {}", mDevice.getAliasOrName());
        binding = ActivityAboutDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.aboutDeviceToolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.aboutDeviceName.setText(mDevice.getAliasOrName());

        if (mDevice.getModel() != null && !mDevice.getModel().isEmpty()) {
            binding.aboutDeviceModelNumber.setText(mDevice.getModel());
        } else {
            binding.aboutDeviceModelNumberLayout.setVisibility(View.GONE);
        }

        binding.aboutDeviceProductName.setText(mDevice.getDeviceCoordinator().getDeviceNameResource());
        binding.aboutDeviceManufacturer.setText(mDevice.getDeviceCoordinator().getManufacturer());
        binding.aboutDeviceNameEdit.setOnClickListener(v -> onEditButtonClicked(this));
    }

    private void onEditButtonClicked(Context context) {
        // Show AlertDialog with EditText to edit the device name
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.controlcenter_set_alias);

        // Set up the input
        final EditText input = new EditText(context);
        input.setTextColor(context.getResources().getColor(dev.oneuiproject.oneui.design.R.color.oui_primary_text_color));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(mDevice.getAliasOrName());
        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = context.getResources().getDimensionPixelSize(R.dimen.fab_margin);
        params.rightMargin = context.getResources().getDimensionPixelSize(R.dimen.fab_margin);
        params.topMargin = context.getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        // Set up the buttons
        builder.setPositiveButton("Rename", (dialog, which) -> {
            try (DBHandler dbHandler = WearableApplication.acquireDB()) {
                DaoSession session = dbHandler.getDaoSession();
                Device dbDevice = DBHelper.getDevice(mDevice, session);
                String alias = input.getText().toString();
                dbDevice.setAlias(alias);
                dbDevice.update();
                mDevice.setAlias(alias);
                binding.aboutDeviceName.setText(alias);
                WearableApplication.setLastDevice(mDevice);
            } catch (Exception ex) {
                LOG.debug("Error setting alias: {}", ex.getMessage());
                GB.toast(context, context.getString(R.string.error_setting_alias) + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
            } finally {
                Intent refreshIntent = new Intent(GBDevice.ACTION_DEVICE_CHANGED);
                refreshIntent.putExtra(GBDevice.EXTRA_DEVICE, mDevice);
                LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);
            }
        });

        builder.setNegativeButton(
            dev.oneuiproject.oneui.design.R.string.oui_common_cancel,
            (dialog, which) -> dialog.cancel()
        );

        builder.show();
    }
}