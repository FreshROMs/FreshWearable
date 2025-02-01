/*  Copyright (C) 2020-2024 Andreas Shimokawa, Arjan Schrijver, Daniel Dakhno,
    José Rebelo, Petr Vaněk

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

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.*;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;
import androidx.gridlayout.widget.GridLayout;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.fit.FitViewerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummarySimpleEntry;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryItems;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryJsonSummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.util.ActivitySummaryUtils;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.SwipeEvents;
import xyz.tenseventyseven.fresh.common.AbstractActionBarActivity;

public class ActivitySummaryDetail extends AbstractActionBarActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryDetail.class);
    BaseActivitySummary currentItem = null;
    private GBDevice gbDevice;
    private Menu mOptionsMenu;
    List<String> filesGpxList = new ArrayList<>();
    int selectedGpxIndex;
    String selectedGpxFile;
    File export_path = null;

    private ActivitySummariesChartFragment activitySummariesChartFragment;
    private ActivitySummariesGpsFragment activitySummariesGpsFragment;

    private final WorkoutValueFormatter workoutValueFormatter = new WorkoutValueFormatter();

    public static int getAlternateColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.alternate_row_background, typedValue, true);
        return typedValue.data;
    }

    public static Bitmap getScreenShot(View view, int height, int width, Context context) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Application.getWindowBackgroundColor(context));
        view.draw(canvas);
        return bitmap;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context appContext = this.getApplicationContext();
        if (appContext instanceof Application) {
            setContentView(R.layout.activity_summary_details);
        }


        Intent intent = getIntent();

        Bundle bundle = intent.getExtras();
        gbDevice = bundle.getParcelable(GBDevice.EXTRA_DEVICE);
        final int position = bundle.getInt("position", 0);
        final int activityFilter = bundle.getInt("activityFilter", 0);
        final long dateFromFilter = bundle.getLong("dateFromFilter", 0);
        final long dateToFilter = bundle.getLong("dateToFilter", 0);
        final long deviceFilter = bundle.getLong("deviceFilter", 0);
        final String nameContainsFilter = bundle.getString("nameContainsFilter");
        final List<Long> itemsFilter = (List<Long>) bundle.getSerializable("itemsFilter");

        final ActivitySummaryItems items = new ActivitySummaryItems(this, gbDevice, activityFilter, dateFromFilter, dateToFilter, nameContainsFilter, deviceFilter, itemsFilter);
        final ScrollView layout = findViewById(R.id.activity_summary_detail_scroll_layout);
        //final LinearLayout layout = findViewById(R.id.activity_summary_detail_relative_layout);

        final Animation animFadeRight;
        final Animation animFadeLeft;
        final Animation animBounceLeft;
        final Animation animBounceRight;

        animFadeRight = AnimationUtils.loadAnimation(
                this,
                R.anim.flyright);
        animFadeLeft = AnimationUtils.loadAnimation(
                this,
                R.anim.flyleft);
        animBounceLeft = AnimationUtils.loadAnimation(
                this,
                R.anim.bounceleft);
        animBounceRight = AnimationUtils.loadAnimation(
                this,
                R.anim.bounceright);

        activitySummariesChartFragment = new ActivitySummariesChartFragment();
        activitySummariesGpsFragment = new ActivitySummariesGpsFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.chartsFragmentHolder, activitySummariesChartFragment)
                .replace(R.id.gpsFragmentHolder, activitySummariesGpsFragment)
                .commit();

        layout.setOnTouchListener(new SwipeEvents(this) {
            @Override
            public void onSwipeRight() {
                BaseActivitySummary newItem = items.getNextItem();
                if (newItem != null) {
                    currentItem = newItem;
                    refreshFromCurrentItem();
                    layout.startAnimation(animFadeRight);
                } else {
                    layout.startAnimation(animBounceRight);
                }
            }

            @Override
            public void onSwipeLeft() {
                BaseActivitySummary newItem = items.getPrevItem();
                if (newItem != null) {
                    currentItem = newItem;
                    refreshFromCurrentItem();
                    layout.startAnimation(animFadeLeft);
                } else {
                    layout.startAnimation(animBounceLeft);
                }
            }
        });

        currentItem = items.getItem(position);
        if (currentItem != null) {
            refreshFromCurrentItem();
        }

        //allows long-press.switch of data being in raw form or recalculated
        ImageView activity_icon = findViewById(R.id.item_image);
        activity_icon.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                workoutValueFormatter.toggleRawData();
                if (currentItem != null) {
                    makeSummaryHeader(currentItem);
                    makeSummaryContent(currentItem);
                }
                return false;
            }
        });
    }

    private void makeSummaryHeader(BaseActivitySummary item) {
        //make view of data from main part of item
        String activitykindname = ActivityKind.fromCode(item.getActivityKind()).getLabel(getApplicationContext());
        String activityname = item.getName();
        Date starttime = item.getStartTime();
        Date endtime = item.getEndTime();
        String durationhms = DateTimeUtils.formatDurationHoursMinutes((endtime.getTime() - starttime.getTime()), TimeUnit.MILLISECONDS);

        ImageView activity_icon = findViewById(R.id.item_image);
        activity_icon.setImageResource(ActivityKind.fromCode(item.getActivityKind()).getIcon());

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(activitykindname);
        }

        TextView activity_name = findViewById(R.id.activityname);
        activity_name.setText(activityname);
        if (StringUtils.isBlank(activityname)) {
            activity_name.setVisibility(View.GONE);
        } else {
            activity_name.setVisibility(View.VISIBLE);
        }

        TextView activityDateTextView = findViewById(R.id.activitydate);

        final Context context = activityDateTextView.getContext();
        final String timeString;
        if (DateTimeUtils.isSameDay(starttime, endtime)) {
            timeString = context.getString(
                    R.string.date_placeholders__start_time__end_time,
                    DateTimeUtils.formatDateTimeRelative(context, starttime),
                    DateTimeUtils.formatTime(endtime.getHours(), endtime.getMinutes())
            );
        } else {
            timeString = context.getString(
                    R.string.date_placeholders__start_time__end_time,
                    DateTimeUtils.formatDateTimeRelative(context, starttime),
                    DateTimeUtils.formatDateTimeRelative(context, endtime)
            );
        }
        activityDateTextView.setText(timeString);

        TextView activityTimeTextView = findViewById(R.id.activityduration);
        activityTimeTextView.setText(durationhms);
    }

    private void refreshFromCurrentItem() {
        makeSummaryHeader(currentItem);
        makeSummaryContent(currentItem);

        activitySummariesChartFragment.setDateAndGetData(
                getTrackFile(),
                getGBDevice(currentItem.getDevice()),
                currentItem.getStartTime().getTime() / 1000,
                currentItem.getEndTime().getTime() / 1000
        );

        if (itemHasGps()) {
            showGpsCanvas();
            activitySummariesGpsFragment.set_data(getTrackFile());
        } else {
            hideGpsCanvas();
        }

        updateMenuItems();
    }

    private File get_path() {
        File path = null;
        try {
            path = FileUtils.getExternalFilesDir();
        } catch (IOException e) {
            LOG.error("Error getting path", e);
        }
        return path;
    }

    private List<String> get_gpx_file_list() {
        List<String> list = new ArrayList<>();

        File[] fileListing = export_path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getPath().toLowerCase().endsWith(".gpx");
            }
        });

        if (fileListing != null && fileListing.length > 1) {
            Arrays.sort(fileListing, new Comparator<File>() {
                @Override
                public int compare(File fileA, File fileB) {
                    if (fileA.lastModified() < fileB.lastModified()) {
                        return 1;
                    }
                    if (fileA.lastModified() > fileB.lastModified()) {
                        return -1;
                    }
                    return 0;
                }
            });
        }

        list.add(getString(R.string.activity_summary_detail_clear_gpx_track));

        for (File file : fileListing) {
            list.add(file.getName());
        }
        return list;
    }

    private void makeSummaryContent(BaseActivitySummary item) {
        final DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
        final ActivitySummaryParser summaryParser = coordinator.getActivitySummaryParser(gbDevice, this);

        //make view of data from summaryData of item
        LinearLayout fieldLayout = findViewById(R.id.summaryDetails);
        fieldLayout.removeAllViews(); //remove old widgets
        ActivitySummaryJsonSummary activitySummaryJsonSummary = new ActivitySummaryJsonSummary(summaryParser, item);
        Map<String, List<Pair<String, ActivitySummaryEntry>>> data = activitySummaryJsonSummary.getSummaryGroupedList(); //get list, grouped by groups
        if (data == null) return;

        for (final Map.Entry<String, List<Pair<String, ActivitySummaryEntry>>> group : data.entrySet()) {
            final String groupKey = group.getKey();
            final List<Pair<String, ActivitySummaryEntry>> entries = group.getValue();

            TableRow label_row = new TableRow(ActivitySummaryDetail.this);
            TextView label_field = new TextView(ActivitySummaryDetail.this);
            label_field.setId(View.generateViewId());
            label_field.setTextSize(18);
            label_field.setPaddingRelative(dpToPx(16), dpToPx(16), 0, dpToPx(16));
            label_field.setTypeface(null, Typeface.BOLD);
            label_field.setText(workoutValueFormatter.getStringResourceByName(groupKey));
            label_row.addView(label_field);
            fieldLayout.addView(label_row);

            GridLayout gridLayout = new GridLayout(ActivitySummaryDetail.this);
            gridLayout.setBackgroundColor(getResources().getColor(R.color.gauge_line_color));
            gridLayout.setColumnCount(2);
            gridLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            int totalCells = entries.stream().mapToInt(e -> e.getRight().getColumnSpan()).sum();
            totalCells += totalCells & 1; // round up to nearest even number, since we have 2 columns
            int cellNumber = 0;
            for (final Pair<String, ActivitySummaryEntry> entry : entries) {
                final int columnSpan = entry.getRight().getColumnSpan();
                if (columnSpan == 2 && cellNumber % 2 != 0) {
                    // This entry needs 2 columns, so let's move to the next row
                    cellNumber++;
                }
                LinearLayout linearLayout = generateLinearLayout(cellNumber, cellNumber + 2 >= totalCells, columnSpan);
                entry.getRight().populate(entry.getLeft(), linearLayout, workoutValueFormatter);
                gridLayout.addView(linearLayout);
                cellNumber += columnSpan;
            }
            if (gridLayout.getChildCount() > 0) {
                if (cellNumber % 2 != 0) {
                    final LinearLayout emptyLayout = generateLinearLayout(cellNumber, true, 1);
                    new ActivitySummarySimpleEntry(null, "", "string").populate("", emptyLayout, workoutValueFormatter);
                    gridLayout.addView(emptyLayout);
                }
                fieldLayout.addView(gridLayout);
            }
        }
    }

    public LinearLayout generateLinearLayout(int i, boolean lastRow, int columnSize) {
        LinearLayout linearLayout = new LinearLayout(ActivitySummaryDetail.this);
        GridLayout.LayoutParams columnParams = new GridLayout.LayoutParams();
        columnParams.columnSpec = GridLayout.spec(i % 2 == 0 ? 0 : 1, 1);
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, columnSize, GridLayout.FILL, 1f)
        );
        layoutParams.width = 0;
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setPadding(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15));
        linearLayout.setBackgroundColor(Application.getWindowBackgroundColor(ActivitySummaryDetail.this));
        int marginLeft = 0;
        int marginTop = 0;
        int marginBottom = 0;
        int marginRight = 0;
        if (i % 2 == 0) {
            marginTop = 2;
            marginRight = 1;
        }
        if (i % 2 == 1) {
            marginTop = 2;
            marginLeft = 1;
        }
        if (lastRow) {
            marginBottom = 2;
        }
        layoutParams.setMargins(dpToPx(marginLeft), dpToPx(marginTop), dpToPx(marginRight), dpToPx(marginBottom));

        return linearLayout;
    }

    public int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // back button
            finish();
            return true;
        } else if (itemId == R.id.activity_action_take_screenshot) {
            take_share_screenshot(ActivitySummaryDetail.this);
            return true;
        } else if (itemId == R.id.activity_action_show_gpx) {
            viewGpxTrack(ActivitySummaryDetail.this);
            return true;
        } else if (itemId == R.id.activity_action_share_gpx) {
            shareGpxTrack(ActivitySummaryDetail.this);
            return true;
        } else if (itemId == R.id.activity_action_dev_inspect_file) {
            final Intent inspectFileIntent = new Intent(ActivitySummaryDetail.this, FitViewerActivity.class);
            inspectFileIntent.putExtra(FitViewerActivity.EXTRA_PATH, currentItem.getRawDetailsPath());
            startActivity(inspectFileIntent);
            return true;
        } else if (itemId == R.id.activity_action_dev_share_raw_summary) {
            shareRawSummary(ActivitySummaryDetail.this, currentItem);
            return true;
        } else if (itemId == R.id.activity_action_dev_share_raw_details) {
            shareRawDetails(ActivitySummaryDetail.this, currentItem);
            return true;
        } else if (itemId == R.id.activity_action_dev_share_json_details) {
            shareJsonDetails(ActivitySummaryDetail.this, currentItem);
            return true;
        } else if (itemId == R.id.activity_summary_detail_action_edit_name) {
            editLabel();
            return true;
        } else if (itemId == R.id.activity_summary_detail_action_edit_gps) {
            editGps();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void editLabel() {
        final EditText input = new EditText(ActivitySummaryDetail.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        String name = currentItem.getName();
        input.setText((name != null) ? name : "");
        FrameLayout container = new FrameLayout(ActivitySummaryDetail.this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(ActivitySummaryDetail.this)
                .setView(container)
                .setCancelable(true)
                .setTitle(ActivitySummaryDetail.this.getString(R.string.activity_summary_edit_name_title))
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String name1 = input.getText().toString();
                    if (name1.isEmpty()) name1 = null;
                    currentItem.setName(name1);
                    currentItem.update();
                    makeSummaryHeader(currentItem);
                    makeSummaryContent(currentItem);
                })
                .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                    // do nothing
                })
                .show();
    }

    private void editGps() {
        export_path = get_path();
        filesGpxList = get_gpx_file_list();

        AlertDialog.Builder builder = new AlertDialog.Builder(ActivitySummaryDetail.this);
        builder.setTitle(R.string.activity_summary_detail_select_gpx_track);
        ArrayAdapter<String> directory_listing = new ArrayAdapter<>(ActivitySummaryDetail.this, android.R.layout.simple_list_item_1, filesGpxList);
        builder.setSingleChoiceItems(directory_listing, 0, (dialog, which) -> {
            selectedGpxIndex = which;
            selectedGpxFile = export_path + "/" + filesGpxList.get(selectedGpxIndex);
            String message = String.format("%s %s?", getString(R.string.set), filesGpxList.get(selectedGpxIndex));
            if (selectedGpxIndex == 0) {
                selectedGpxFile = null;
                message = String.format("%s?", getString(R.string.activity_summary_detail_clear_gpx_track));
            }

            new AlertDialog.Builder(ActivitySummaryDetail.this)
                    .setCancelable(true)
                    .setIcon(R.drawable.ic_warning)
                    .setTitle(R.string.activity_summary_detail_editing_gpx_track)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, (dialog1, which1) -> {
                        currentItem.setGpxTrack(selectedGpxFile);
                        currentItem.update();
                        if (itemHasGps()) {
                            showGpsCanvas();
                            activitySummariesGpsFragment.set_data(getTrackFile());
                        } else {
                            hideGpsCanvas();
                        }
                    })
                    .setNegativeButton(R.string.Cancel, (dialog2, which2) -> {
                    })
                    .show();
            dialog.dismiss();
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void take_share_screenshot(Context context) {
        final ScrollView layout = findViewById(R.id.activity_summary_detail_scroll_layout);
        final int width = layout.getChildAt(0).getWidth();
        final int height = layout.getChildAt(0).getHeight();
        final Bitmap screenShot = getScreenShot(layout, height, width, context);

        final String fileName = FileUtils.makeValidFileName("Screenshot-" + ActivityKind.fromCode(currentItem.getActivityKind()).getLabel(context).toLowerCase() + "-" + DateTimeUtils.formatIso8601(currentItem.getStartTime()) + ".png");
        try {
            final File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
            final FileOutputStream fOut = new FileOutputStream(targetFile);
            screenShot.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
            shareScreenshot(targetFile, context);
            GB.toast(getApplicationContext(), "Screenshot saved", Toast.LENGTH_LONG, GB.INFO);
        } catch (IOException e) {
            LOG.error("Error getting screenshot", e);
        }
    }

    private void shareScreenshot(File targetFile, Context context) {
        Uri contentUri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".screenshot_provider", targetFile);
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("image/*");
        String shareBody = "Sports Activity";
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Sports Activity");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

        try {
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.activity_error_no_app_for_png, Toast.LENGTH_LONG).show();
        }
    }

    private void viewGpxTrack(Context context) {
        final File gpxFile = ActivitySummaryUtils.getGpxFile(currentItem);
        if (gpxFile == null) {
            GB.toast(getApplicationContext(), "No GPX track in this activity", Toast.LENGTH_LONG, GB.INFO);
            return;
        }

        try {
            AndroidUtils.viewFile(gpxFile.getPath(), "application/gpx+xml", context);
        } catch (final Exception e) {
            GB.toast(getApplicationContext(), "Unable to display GPX track: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void shareGpxTrack(final Context context) {
        final File gpxFile = ActivitySummaryUtils.getGpxFile(currentItem);
        if (gpxFile == null) {
            GB.toast(getApplicationContext(), "No GPX track in this activity", Toast.LENGTH_LONG, GB.INFO);
            return;
        }

        try {
            AndroidUtils.shareFile(context, gpxFile);
        } catch (final Exception e) {
            GB.toast(context, "Unable to share GPX track: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private static void shareRawSummary(final Context context, final BaseActivitySummary summary) {
        if (summary.getRawSummaryData() == null) {
            GB.toast(context, "No raw summary in this activity", Toast.LENGTH_LONG, GB.WARN);
            return;
        }

        final String rawSummaryFilename = FileUtils.makeValidFileName(String.format("%s_summary.bin", DateTimeUtils.formatIso8601(summary.getStartTime())));

        try {
            AndroidUtils.shareBytesAsFile(context, rawSummaryFilename, summary.getRawSummaryData());
        } catch (final Exception e) {
            GB.toast(context, "Unable to share GPX track: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private static void shareRawDetails(final Context context, final BaseActivitySummary summary) {
        if (summary.getRawDetailsPath() == null) {
            GB.toast(context, "No raw details in this activity", Toast.LENGTH_LONG, GB.WARN);
            return;
        }

        try {
            AndroidUtils.shareFile(context, new File(summary.getRawDetailsPath()));
        } catch (final Exception e) {
            GB.toast(context, "Unable to share raw details: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private static void shareJsonDetails(final Context context, final BaseActivitySummary summary) {
        if (summary.getSummaryData() == null) {
            GB.toast(context, "No json details in this activity", Toast.LENGTH_LONG, GB.WARN);
            return;
        }

        final String jsonSummaryFilename = FileUtils.makeValidFileName(String.format("%s.json", DateTimeUtils.formatIso8601(summary.getStartTime())));

        try {
            AndroidUtils.shareBytesAsFile(context, jsonSummaryFilename, summary.getSummaryData().getBytes(StandardCharsets.UTF_8));
        } catch (final Exception e) {
            GB.toast(context, "Unable to share json details: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void updateMenuItems() {
        boolean hasGpx = false;
        boolean hasRawSummary = false;
        boolean hasRawDetails = false;

        if (currentItem != null) {
            hasGpx = itemHasGps();
            hasRawSummary = currentItem.getRawSummaryData() != null;

            final String rawDetailsPath = currentItem.getRawDetailsPath();
            if (rawDetailsPath != null) {
                hasRawDetails = new File(rawDetailsPath).exists();
            }
        }

        if (mOptionsMenu != null) {
            final SubMenu overflowMenu = mOptionsMenu.findItem(R.id.activity_detail_overflowMenu).getSubMenu();
            if (overflowMenu != null) {
                overflowMenu.findItem(R.id.activity_action_show_gpx).setVisible(hasGpx);
                overflowMenu.findItem(R.id.activity_action_share_gpx).setVisible(hasGpx);
                overflowMenu.findItem(R.id.activity_action_dev_inspect_file).setVisible(hasRawDetails && currentItem.getRawDetailsPath().toLowerCase(Locale.ROOT).endsWith(".fit"));
                overflowMenu.findItem(R.id.activity_action_dev_share_raw_summary).setVisible(hasRawSummary);
                overflowMenu.findItem(R.id.activity_action_dev_share_raw_details).setVisible(hasRawDetails);
                final MenuItem devToolsMenu = overflowMenu.findItem(R.id.activity_action_dev_tools);
                final SubMenu devToolsSubMenu = devToolsMenu.getSubMenu();
                devToolsMenu.setVisible(devToolsSubMenu != null && devToolsSubMenu.hasVisibleItems());
            }
        }
    }

    private void showGpsCanvas() {
        final View gpsView = findViewById(R.id.gpsFragmentHolder);
        final ViewGroup.LayoutParams params = gpsView.getLayoutParams();
        params.height = (int) (300 * getApplicationContext().getResources().getDisplayMetrics().density);
        gpsView.setLayoutParams(params);
    }

    private void hideGpsCanvas() {
        final View gpsView = findViewById(R.id.gpsFragmentHolder);
        final ViewGroup.LayoutParams params = gpsView.getLayoutParams();
        params.height = 0;
        gpsView.setLayoutParams(params);
    }

    private boolean itemHasGps() {
        if (currentItem.getGpxTrack() != null) {
            final File existing = FileUtils.tryFixPath(new File(currentItem.getGpxTrack()));
            if (existing != null && existing.canRead()) {
                return true;
            }
        }
        final String summaryDataJson = currentItem.getSummaryData();
        if (summaryDataJson != null && summaryDataJson.contains(INTERNAL_HAS_GPS)) {
            final ActivitySummaryData summaryData = ActivitySummaryData.fromJson(summaryDataJson);
            return summaryData != null && summaryData.getBoolean(INTERNAL_HAS_GPS, false);
        }

        return false;
    }

    @Nullable
    private File getTrackFile() {
        return ActivitySummaryUtils.getTrackFile(currentItem);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.activity_take_screenshot_menu, menu);
        updateMenuItems();
        return true;
    }

    @Nullable
    private static GBDevice getGBDevice(final Device findDevice) {
        return Application.app().getDeviceManager().getDevices()
                .stream()
                .filter(d -> d.getAddress().equalsIgnoreCase(findDevice.getIdentifier()))
                .findFirst()
                .orElse(null);
    }
}
