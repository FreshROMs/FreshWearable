package xyz.tenseventyseven.fresh.wearable.activities.devicesettings.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import dev.oneuiproject.oneui.layout.ToolbarLayout;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.common.AbstractNoActionBarActivity;
import xyz.tenseventyseven.fresh.databinding.WearActivityAlarmsBinding;
import xyz.tenseventyseven.fresh.wearable.adapters.AlarmListAdapter;

public class AlarmsActivity extends AbstractNoActionBarActivity implements MenuProvider, ToolbarLayout.ActionModeListener, AlarmListAdapter.AlarmListAdapterListener {
    private static final int REQ_CONFIGURE_ALARM = 1;
    private WearActivityAlarmsBinding binding;
    private AlarmListAdapter adapter;
    private GBDevice device;
    private boolean isSyncDisabled;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = WearActivityAlarmsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            finish();
            return;
        }

        adapter = new AlarmListAdapter(this, device);
        adapter.setListener(this);
        adapter.getSelectionState().observe(this, (state) -> binding.toolbar.updateAllSelector(state.getCount(), true, state.isAllSelected()));

        binding.alarmList.setLayoutManager(new LinearLayoutManager(this));
        binding.alarmList.setAdapter(adapter);
        binding.swipeRefreshLayout.setRefreshing(true);
        binding.swipeRefreshLayout.setOnRefreshListener(this::requestUpdate);

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(DeviceService.ACTION_SAVE_ALARMS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        binding.toolbar.setTitle(getString(R.string.wear_device_alarms_title));
        addMenuProvider(this);

        requestUpdate();
    }

    @Override
    protected void onPause() {
        if (!isSyncDisabled && device.isInitialized()) {
            sendAlarmsToDevice();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CONFIGURE_ALARM) {
            isSyncDisabled = false;
            updateAlarmsFromDB();

            if (device.isInitialized()) {
                sendAlarmsToDevice();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            binding.toolbar.endActionMode();
            return;
        }

        super.onBackPressed();
    }

    private void requestUpdate() {
        // For UX reasons, wait 1 second before requesting alarms
        invalidateOptionsMenu();
        binding.swipeRefreshLayout.postDelayed(() -> Application.deviceService(device).onRequestAlarms(), 1000);
    }

    private void updateAlarmsFromDB() {
        List<Alarm> alarms = DBHelper.getAlarms(device);
        if (alarms.isEmpty()) {
            alarms = AlarmUtils.readAlarmsFromPrefs(device);
            storeMigratedAlarms(alarms);
        }

        // Sort alarms by time
        alarms.sort((a, b) -> {
            int hour1 = a.getHour();
            int hour2 = b.getHour();
            if (hour1 != hour2) {
                return Integer.compare(hour1, hour2);
            }

            return Integer.compare(a.getMinute(), b.getMinute());
        });

        adapter.setItems(alarms);
        binding.swipeRefreshLayout.setRefreshing(false);
        if (alarms.isEmpty()) {
            binding.alarmNoItems.setVisibility(View.VISIBLE);
            binding.alarmList.setVisibility(View.GONE);
        } else {
            binding.alarmList.setVisibility(View.VISIBLE);
            binding.alarmNoItems.setVisibility(View.GONE);
        }
        invalidateOptionsMenu();
    }

    private void storeMigratedAlarms(List<Alarm> alarms) {
        for (Alarm alarm : alarms) {
            DBHelper.store(alarm);
        }
    }

    private void onAddAlarm() {
        List<Alarm> alarms = adapter.getItems();
        int maxAlarms = device.getDeviceCoordinator().getAlarmSlotCount(device);

        if (alarms.size() >= maxAlarms) {
            // No more alarms can be added
            return;
        }

        // Find a free slot
        try (DBHandler db = Application.acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            for (int i = 0; i < maxAlarms; i++) {
                boolean found = false;
                for (Alarm alarm : alarms) {
                    if (alarm.getPosition() == i) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    Alarm alarm = AlarmUtils.createDefaultAlarm(daoSession, device, i);
                    onClickAlarm(alarm);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e("AlarmsActivity", "Error adding alarm", e);
        }
    }

    @Override
    public void onLongClickAlarm(Alarm alarm) {
        adapter.setEditMode(alarm);
    }

    @Override
    public void onEditStateChange(boolean isEditMode) {
        this.isEditMode = isEditMode;
        if (isEditMode) {
            binding.toolbar.startActionMode(this);
        } else {
            sendAlarmsToDevice();
        }
    }

    public void onClickAlarm(Alarm alarm) {
        isSyncDisabled = true;
        Intent startIntent = new Intent(getApplicationContext(), AlarmDetailActivity.class);
        startIntent.putExtra(Alarm.EXTRA_ALARM, alarm);
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
        startActivityForResult(startIntent, REQ_CONFIGURE_ALARM);
    }

    private void sendAlarmsToDevice(ArrayList<Alarm> alarms) {
        Application.deviceService(device).onSetAlarms(alarms);
    }

    private void sendAlarmsToDevice() {
        sendAlarmsToDevice(adapter.getItems());
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (action.equals(DeviceService.ACTION_SAVE_ALARMS)) {
                updateAlarmsFromDB();
            }
        }
    };

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.wear_alarms_menu, menu);

        boolean enabled = !binding.swipeRefreshLayout.isRefreshing() && adapter.getItems().size() < device.getDeviceCoordinator().getAlarmSlotCount(device);
        menu.findItem(R.id.action_add_alarm).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.action_add_alarm).setEnabled(enabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menu.findItem(R.id.action_add_alarm).setIconTintList(getColorStateList(enabled ? R.color.wearable_header_title : R.color.wearable_secondary_text));
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.action_add_alarm) {
            onAddAlarm();
            return true;
        } else if (id == R.id.action_edit_alarms) {
            adapter.setEditMode(true);
            return true;
        }
        return false;
    }

    @Override
    public void onInflateActionMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.wear_alarms_edit_menu, menu);
        menu.findItem(R.id.action_delete).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_WITH_TEXT|MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.action_turn_on_alarms).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_WITH_TEXT|MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.action_turn_off_alarms).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_WITH_TEXT|MenuItem.SHOW_AS_ACTION_ALWAYS);

        // Check if any of the selected alarms are enabled or disabled
        boolean hasEnabled = adapter.getSelectedItems().stream().anyMatch(Alarm::getEnabled);
        boolean hasDisabled = adapter.getSelectedItems().stream().anyMatch(alarm -> !alarm.getEnabled());

        menu.findItem(R.id.action_turn_on_alarms).setVisible(hasDisabled);
        menu.findItem(R.id.action_turn_off_alarms).setVisible(hasEnabled);
    }

    @Override
    public void onEndActionMode() {
        adapter.setEditMode(false);
    }

    @Override
    public boolean onMenuItemClicked(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        ArrayList<Alarm> selectedAlarms = adapter.getSelectedItems();
        if (id == R.id.action_delete) {
            for (Alarm alarm : selectedAlarms) {
                alarm.setUnused(true);
            }
        } else if (id == R.id.action_turn_on_alarms) {
            for (Alarm alarm : selectedAlarms) {
                alarm.setUnused(false);
                alarm.setEnabled(true);
            }
        } else if (id == R.id.action_turn_off_alarms) {
            for (Alarm alarm : selectedAlarms) {
                alarm.setUnused(false);
                alarm.setEnabled(false);
            }
        }

        sendAlarmsToDevice(selectedAlarms);
        binding.toolbar.endActionMode();
        binding.swipeRefreshLayout.setRefreshing(true);
        requestUpdate();
        return true;
    }

    @Override
    public void onSelectAll(boolean enabled) {
        adapter.setEditMode(true, enabled);
        binding.toolbar.updateAllSelector(enabled ? adapter.getItems().size() : 0, true, enabled);
    }
}