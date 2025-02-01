/*  Copyright (C) 2017-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniele Gobbetti, José Rebelo, Petr Vaněk

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.picker.app.SeslDatePickerDialog;
import androidx.picker.widget.SeslDatePicker;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.ActivitySummariesAdapter;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.ActivitySummaryUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class ActivitySummariesActivity extends AbstractListActivity<BaseActivitySummary> {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesActivity.class);

    static final int ACTIVITY_FILTER = 1;
    static final int ACTIVITY_DETAIL = 11;
    HashMap<String, ActivityKind> activityKindMap = new HashMap<>(0);
    int activityFilter = 0;
    long dateFromFilter = 0;
    long dateToFilter = 0;
    long deviceFilter;
    List<Long> itemsFilter;
    String nameContainsFilter;
    private GBDevice mGBDevice;
    private BitSet selectedItems;
    private SwipeRefreshLayout swipeLayout;
    private ActionMode mActionMode;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case GBDevice.ACTION_DEVICE_CHANGED:
                    GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (device == null) {
                        LOG.error("Got device changed without device");
                        return;
                    }
                    if (!device.equals(mGBDevice)) {
                        return;
                    }
                    if (device.isBusy()) {
                        swipeLayout.setRefreshing(true);
                    } else {
                        boolean wasBusy = swipeLayout.isRefreshing();
                        swipeLayout.setRefreshing(false);
                        if (wasBusy) {
                            refresh();
                        }
                    }
                    break;
            }
        }
    };

    public static int getBackgroundColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.sports_activity_summary_background, typedValue, true);
        return typedValue.data;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.activity_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // back button, close drawer if open, otherwise exit
            finish();
            return true;
        } else if (itemId == R.id.activity_action_manage_timestamp) {
            resetFetchTimestampToChosenDate();
            return true;
        } else if (itemId == R.id.activity_action_filter) {
            runFilterActivity();
            return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == ACTIVITY_FILTER && resultData != null) {
            Bundle bundle = resultData.getExtras();
            activityFilter = bundle.getInt("activityFilter", 0);
            dateFromFilter = bundle.getLong("dateFromFilter", 0);
            dateToFilter = bundle.getLong("dateToFilter", 0);
            deviceFilter = bundle.getLong("deviceFilter", 0);
            nameContainsFilter = bundle.getString("nameContainsFilter");
            itemsFilter = (List<Long>) bundle.getSerializable("itemsFilter");
            setActivityKindFilter(activityFilter);
            setDateFromFilter(dateFromFilter);
            setDateToFilter(dateToFilter);
            setNameContainsFilter(nameContainsFilter);
            setItemsFilter(itemsFilter);
            setDeviceFilter(deviceFilter);
            refresh();
        }
        if (requestCode == ACTIVITY_DETAIL) {
            refresh();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }
        deviceFilter = getDeviceId(mGBDevice);
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        super.onCreate(savedInstanceState);

        ActivitySummariesAdapter activitySummariesAdapter = new ActivitySummariesAdapter(this, mGBDevice, activityFilter, dateFromFilter, dateToFilter, nameContainsFilter, deviceFilter, itemsFilter);
        selectedItems = activitySummariesAdapter.getSelectedItems();

        activitySummariesAdapter.setOnItemClickListener(position -> {
            if (!selectedItems.isEmpty()) {
                selectedItems.set(position, !selectedItems.get(position));
                activitySummariesAdapter.notifyItemChanged(position);
                if (!selectedItems.isEmpty()) {
                    startActionMode();
                } else {
                    stopActionMode();
                }
                return;
            }
            if (position == 0) return; // item 0 is empty for dashboard
            ActivitySummary summary = activitySummariesAdapter.getItem(position);
            if (summary != null) {
                try {
                    showActivityDetail(position);
                } catch (Exception e) {
                    GB.toast(getApplicationContext(), "Unable to display Activity Detail, maybe the activity is not available yet: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
                }
            }
        });
        activitySummariesAdapter.setOnItemLongClickListener(position -> {
            selectedItems.set(position, !selectedItems.get(position));
            activitySummariesAdapter.notifyItemChanged(position);

            if (!selectedItems.isEmpty()) {
                startActionMode();
            } else {
                stopActionMode();
            }
        });
        setItemAdapter(activitySummariesAdapter);

        swipeLayout = findViewById(R.id.list_activity_swipe_layout);
        swipeLayout.setOnRefreshListener(this::fetchTrackData);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> fetchTrackData());

        activityKindMap = fillKindMap();
    }

    private void stopActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
    }

    private void startActionMode() {
        int[] numSelected = new int[]{0};
        for (int i = 0; i < selectedItems.length(); i++) {
            if (selectedItems.get(i)) {
                numSelected[0]++;
            }
        }
        if (mActionMode != null) {
            // already in action mode
            mActionMode.setTitle(getString(R.string.number_selected_items, numSelected[0]));
            return;
        }

        mActionMode = startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
                mode.setTitle(getString(R.string.number_selected_items, numSelected[0]));

                getMenuInflater().inflate(R.menu.activity_list_context_menu, menu);
                findViewById(R.id.fab).setVisibility(View.INVISIBLE);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode actionMode, final MenuItem menuItem) {
                boolean processed = false;
                final int itemId = menuItem.getItemId();
                if (itemId == R.id.activity_action_delete) {
                    final List<BaseActivitySummary> toDelete = new ArrayList<>();
                    for (int i = 0; i < selectedItems.length(); i++) {
                        if (selectedItems.get(i)) {
                            toDelete.add(getItemAdapter().getItem(i));
                        }
                    }

                    new AlertDialog.Builder(ActivitySummariesActivity.this)
                            .setTitle(ActivitySummariesActivity.this.getString(R.string.sports_activity_confirm_delete_title, toDelete.size()))
                            .setMessage(ActivitySummariesActivity.this.getString(R.string.sports_activity_confirm_delete_description, toDelete.size()))
                            .setIcon(R.drawable.ic_delete_forever)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int whichButton) {
                                    deleteItems(toDelete);
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .show();

                    processed = true;
                } else if (itemId == R.id.activity_action_export) {
                    final List<String> paths = new ArrayList<>();

                    for (int i = 0; i < selectedItems.length(); i++) {
                        if (selectedItems.get(i)) {
                            BaseActivitySummary summary = getItemAdapter().getItem(i);
                            if (summary != null) {
                                File gpxTrack = ActivitySummaryUtils.getGpxFile(summary);
                                if (gpxTrack != null) {
                                    paths.add(gpxTrack.getPath());
                                }
                            }
                        }
                    }
                    shareMultiple(paths);
                    processed = true;
                } else if (itemId == R.id.activity_action_select_all) {
                    for (int i = 1; i < getItemAdapter().getItemCount() - 1; i++) {
                        if (!selectedItems.get(i)) {
                            numSelected[0]++;
                            selectedItems.set(i, true);
                            getItemAdapter().notifyItemChanged(i);
                        }
                    }
                    actionMode.setTitle(getString(R.string.number_selected_items, numSelected[0]));
                    return true; //don't finish actionMode in this case!
                } else if (itemId == R.id.activity_action_addto_filter) {
                    final List<Long> toFilter = new ArrayList<>();
                    for (int i = 0; i < selectedItems.length(); i++) {
                        if (selectedItems.get(i)) {
                            BaseActivitySummary summary = getItemAdapter().getItem(i);
                            if (summary != null && summary.getId() != null) {
                                Long id = summary.getId();
                                toFilter.add(id);
                            }
                        }
                    }
                    itemsFilter = toFilter;
                    setItemsFilter(itemsFilter);
                    refresh();

                    processed = true;
                }
                actionMode.finish();
                mActionMode = null;
                return processed;
            }

            @Override
            public void onDestroyActionMode(final ActionMode mode) {
                mActionMode = null;
                for (int i = 0; i < selectedItems.length(); i++) {
                    if (selectedItems.get(i)) {
                        selectedItems.set(i, false);
                        getItemAdapter().notifyItemChanged(i);
                    }
                }
                findViewById(R.id.fab).setVisibility(View.VISIBLE);
            }
        });
    }

    private LinkedHashMap<String, ActivityKind> fillKindMap() {
        LinkedHashMap<String, ActivityKind> newMap = new LinkedHashMap<>(0); //reset

        newMap.put(getString(R.string.activity_summaries_all_activities), ActivityKind.UNKNOWN);
        for (BaseActivitySummary item : getItemAdapter().getItems()) {
            String activityName = ActivityKind.fromCode(item.getActivityKind()).getLabel(this);
            if (!newMap.containsKey(activityName) && item.getActivityKind() != 0) {
                newMap.put(activityName, ActivityKind.fromCode(item.getActivityKind()));
            }
        }
        return newMap;
    }

    public void resetFetchTimestampToChosenDate() {
        final Calendar currentDate = Calendar.getInstance();
        new SeslDatePickerDialog(this, new SeslDatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(SeslDatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar date = Calendar.getInstance();
                date.set(year, monthOfYear, dayOfMonth);

                long timestamp = date.getTimeInMillis() - 1000;
                SharedPreferences.Editor editor = Application.getDeviceSpecificSharedPrefs(mGBDevice.getAddress()).edit();
                editor.remove("lastSportsActivityTimeMillis"); //FIXME: key reconstruction is BAD
                editor.putLong("lastSportsActivityTimeMillis", timestamp);
                editor.apply();
            }
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void deleteItems(List<BaseActivitySummary> items) {
        for (BaseActivitySummary item : items) {
            try {
                item.delete();
            } catch (Exception e) {
                //pass delete error
            }
        }
        // Adapter is fully reloaded after refresh
        refresh();
    }

    private void fetchTrackData() {
        if (mGBDevice.isInitialized() && !mGBDevice.isBusy()) {
            swipeLayout.setRefreshing(true);
            Application.deviceService(mGBDevice).onFetchRecordedData(RecordedDataTypes.TYPE_GPS_TRACKS);
        } else {
            swipeLayout.setRefreshing(false);
            if (!mGBDevice.isInitialized()) {
                GB.toast(this, getString(R.string.device_not_connected), Toast.LENGTH_SHORT, GB.ERROR);
            }
        }
    }

    private void shareMultiple(List<String> paths) {

        ArrayList<Uri> uris = new ArrayList<>();
        for (String path : paths) {
            File file = new File(path);
            uris.add(FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".screenshot_provider", file));
        }

        if (!uris.isEmpty()) {
            final Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("application/gpx+xml");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            startActivity(Intent.createChooser(intent, "SHARE"));
        } else {
            GB.toast(this, "No selected activity contains a GPX track to share", Toast.LENGTH_SHORT, GB.ERROR);
        }
    }

    private void showActivityDetail(int position) {
        Intent ActivitySummaryDetailIntent = new Intent(this, ActivitySummaryDetail.class);
        Bundle bundle = new Bundle();

        bundle.putInt("position", position);
        bundle.putSerializable("activityKindMap", activityKindMap);
        bundle.putSerializable("itemsFilter", (Serializable) itemsFilter);
        bundle.putInt("activityFilter", activityFilter);
        bundle.putLong("dateFromFilter", dateFromFilter);
        bundle.putLong("dateToFilter", dateToFilter);
        bundle.putLong("deviceFilter", deviceFilter);
        bundle.putLong("initial_deviceFilter", getDeviceId(mGBDevice));
        bundle.putString("nameContainsFilter", nameContainsFilter);
        ActivitySummaryDetailIntent.putExtras(bundle);

        ActivitySummaryDetailIntent.putExtra(GBDevice.EXTRA_DEVICE, mGBDevice);
        startActivityForResult(ActivitySummaryDetailIntent, ACTIVITY_DETAIL);
    }

    private void runFilterActivity() {
        Intent filterIntent = new Intent(this, ActivitySummariesFilter.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("activityKindMap", activityKindMap);
        bundle.putSerializable("itemsFilter", (Serializable) itemsFilter);
        bundle.putInt("activityFilter", activityFilter);
        bundle.putLong("dateFromFilter", dateFromFilter);
        bundle.putLong("dateToFilter", dateToFilter);
        bundle.putLong("deviceFilter", deviceFilter);
        bundle.putLong("initial_deviceFilter", getDeviceId(mGBDevice));
        bundle.putString("nameContainsFilter", nameContainsFilter);
        filterIntent.putExtras(bundle);
        startActivityForResult(filterIntent, ACTIVITY_FILTER);
    }

    private long getDeviceId(GBDevice device) {
        try (DBHandler handler = Application.acquireDB()) {
            Device dbDevice = DBHelper.findDevice(device, handler.getDaoSession());
            return dbDevice.getId();
        } catch (Exception e) {
        }
        return 0;
    }
}
