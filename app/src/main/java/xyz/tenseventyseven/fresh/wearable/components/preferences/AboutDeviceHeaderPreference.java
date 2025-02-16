package xyz.tenseventyseven.fresh.wearable.components.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.components.HorizontalProgressView;

public class AboutDeviceHeaderPreference extends Preference {
    private GBDevice device;
    private TextView deviceName;
    private TextView manufacturer;
    private TextView productName;
    private LinearLayout modelNumberLayout;
    private TextView modelNumber;

    public AboutDeviceHeaderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AboutDeviceHeaderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AboutDeviceHeaderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AboutDeviceHeaderPreference(Context context) {
        super(context);
    }

    public AboutDeviceHeaderPreference(Context context, GBDevice device) {
        super(context);
        this.device = device;

        // Inflate the layout
        setLayoutResource(R.layout.wear_preference_about_device_header);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (device == null) {
            return;
        }

        deviceName = (TextView) holder.findViewById(R.id.about_device_name);
        manufacturer = (TextView) holder.findViewById(R.id.about_device_manufacturer);
        productName = (TextView) holder.findViewById(R.id.about_device_product_name);
        modelNumberLayout = (LinearLayout) holder.findViewById(R.id.about_device_model_number_layout);
        modelNumber = (TextView) holder.findViewById(R.id.about_device_model_number);

        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        deviceName.setText(device.getAliasOrName());
        manufacturer.setText(coordinator.getManufacturer());
        productName.setText(coordinator.getDeviceNameResource());

        String model = device.getModel();
        if (model != null && !model.isEmpty()) {
            modelNumber.setText(model);
        } else {
            modelNumberLayout.setVisibility(View.GONE);
        }

        // Set up the edit button
        Button editButton = (Button) holder.findViewById(R.id.about_device_name_edit);
        if (editButton != null) {
            editButton.setOnClickListener(v -> onEditButtonClicked(getContext()));
        }
    }

    private void onEditButtonClicked(Context context) {
        // Show AlertDialog with EditText to edit the device name
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.controlcenter_set_alias);

        // Set up the input
        final EditText input = new EditText(context);
        input.setTextColor(context.getResources().getColor(dev.oneuiproject.oneui.design.R.color.oui_primary_text_color));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(device.getAliasOrName());
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
            try (DBHandler dbHandler = Application.acquireDB()) {
                DaoSession session = dbHandler.getDaoSession();
                Device dbDevice = DBHelper.getDevice(device, session);
                String alias = input.getText().toString();
                dbDevice.setAlias(alias);
                dbDevice.update();
                device.setAlias(alias);

                Application.app().setLastDeviceAddress(device.getAddress());
            } catch (Exception ex) {
                GB.toast(context, context.getString(R.string.error_setting_alias) + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
            } finally {
                Intent refreshIntent = new Intent(GBDevice.ACTION_DEVICE_CHANGED);
                refreshIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);
            }
        });

        builder.setNegativeButton(
                dev.oneuiproject.oneui.design.R.string.oui_common_cancel,
                (dialog, which) -> dialog.cancel()
        );

        builder.show();
    }

    // This is the method that is called when the preference is clicked
    @Override
    public void onClick() {
        // Do nothing
    }
}
