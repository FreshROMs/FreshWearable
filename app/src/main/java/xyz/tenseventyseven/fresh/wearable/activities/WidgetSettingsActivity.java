/*  Copyright (C) 2023-2024 José Rebelo

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
package xyz.tenseventyseven.fresh.wearable.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import dev.oneuiproject.oneui.layout.ToolbarLayout;
import me.relex.circleindicator.CircleIndicator3;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetLayout;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPartSubtype;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetType;
import xyz.tenseventyseven.fresh.common.CommonActivityAbstract;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPart;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetScreen;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.wearable.activities.widgetsettings.WidgetPreviewFragment;
import xyz.tenseventyseven.fresh.wearable.adapters.WidgetListItemAdapter;
import xyz.tenseventyseven.fresh.wearable.components.WatchWidgetPreview;

public class WidgetSettingsActivity extends CommonActivityAbstract implements
        AdapterView.OnItemClickListener {
    private static final Logger LOG = LoggerFactory.getLogger(WidgetSettingsActivity.class);
    private GBDevice mDevice;
    private WidgetManager mWidgetManager;

    private ToolbarLayout mToolbar;
    private ViewPager2 mViewPager;

    private FragmentPagerAdapter mPagerAdapter;

    private List<WidgetScreen> mWidgetScreens = new ArrayList<>();

    private ListView mWidgetList;
    private WidgetListItemAdapter mWidgetAdapter;
    private List<WidgetPart> mWidgetParts = new ArrayList<>();
    private WidgetPart mCurrentPart;
    private int mCurrentPartIdx;
    private WidgetScreen mCurrentScreen;
    private CircleIndicator3 mIndicator;

    private final WatchWidgetPreview.OnWidgetItemClickListener mListener = (screen, part, index, selected) -> {
        mCurrentPart = part;
        mCurrentPartIdx = index;
        mCurrentScreen = screen;
        updateAvailableWidgets();
    };

    private final WidgetPreviewFragment.OnDeleteItemClickListener mDeleteListener = this::deleteScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_settings);

        mDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (mDevice == null) {
            LOG.error("device must not be null");
            finish();
            return;
        }

        mWidgetManager = mDevice.getDeviceCoordinator().getWidgetManager(mDevice);
        if (mWidgetManager == null) {
            LOG.error("WidgetManager must not be null");
            finish();
            return;
        }

        mWidgetScreens = mWidgetManager.getWidgetScreens();
        mPagerAdapter = new FragmentPagerAdapter(
                this,
                mWidgetScreens,
                mListener,
                mDeleteListener,
                mWidgetManager.getMinScreens()
        );
        mToolbar = findViewById(R.id.widget_screen_details_toolbar);
        mViewPager = findViewById(R.id.widget_screen_details_pager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.registerOnPageChangeCallback(new OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                mCurrentScreen = mWidgetScreens.get(position);
            }
        });
        mIndicator = findViewById(R.id.widget_screen_details_indicator);
        mIndicator.setViewPager(mViewPager);

        mWidgetAdapter = new WidgetListItemAdapter(this, mWidgetParts);
        mWidgetList = findViewById(R.id.widget_screen_details_list);
        mWidgetList.setAdapter(mWidgetAdapter);
        mWidgetList.setOnItemClickListener(this);

        addMenuProvider(menuProvider);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // back button
            // TODO confirm when exiting without saving
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        WidgetPart part = mWidgetParts.get(position);

        // If the selected part supports subtypes, the user must select a subtype
        List<WidgetPartSubtype> subtypes = part.getSupportedSubtypes();
        if (subtypes != null && !subtypes.isEmpty()) {
            final String[] subtypeStrings = new String[subtypes.size()];
            for (int j = 0; j < subtypes.size(); j++) {
                subtypeStrings[j] = subtypes.get(j).getName();
            }
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subtypeStrings);
            new AlertDialog.Builder(WidgetSettingsActivity.this).setAdapter(adapter, (dialogInterface, i) -> {
                final WidgetPartSubtype selectedSubtype = subtypes.get(i);
                part.setSubtype(selectedSubtype);
                mCurrentScreen.getParts().set(mCurrentPartIdx, part);
                saveWidgetScreen(mCurrentScreen);
            }).setTitle(R.string.widget_subtype).create().show();
            return;
        }

        mCurrentScreen.getParts().set(mCurrentPartIdx, part);
        saveWidgetScreen(mCurrentScreen);
    }

    private List<WidgetPart> getDefaultWidgets(WidgetLayout layout) {
        // Get all widget types supported by the layout
        final ArrayList<WidgetPart> types = new ArrayList<>();
        for (final WidgetType widgetType: layout.getWidgetTypes()) {
            types.add(new WidgetPart(null, "", widgetType));
        }

        // Get a list of widget parts for each type
        final ArrayList<WidgetPart> parts = new ArrayList<>();
        final List<WidgetPart> added = new ArrayList<>();
        for (final WidgetPart type: types) {
            List<WidgetPart> supported = mWidgetManager.getSupportedWidgetParts(type.getType());
            // Find a random supported widget part that is not already added
            // and doesn't have a subtype
            for (int i = 0; i < supported.size(); i++) {
                final int j = (int) (Math.random() * supported.size());
                final WidgetPart part = supported.get(j);
                if (!added.contains(part) && part.getSupportedSubtypes().isEmpty()) {
                    parts.add(part);
                    added.add(part);
                    break;
                }
            }
        }

        return parts;
    }

    private void addScreen() {
        if (mWidgetManager.getMaxScreens() <= mWidgetManager.getWidgetScreens().size()) {
            Toast.makeText(this, "Max limit reached", Toast.LENGTH_SHORT).show();
            return;
        }

        final WidgetLayout layout = mWidgetManager.getSupportedWidgetLayouts().get(0);
        final List<WidgetPart> parts = getDefaultWidgets(layout);
        final WidgetScreen screen = new WidgetScreen(null, layout, parts);

        mCurrentScreen = screen;
        mWidgetScreens.add(screen);
        mPagerAdapter.notifyDataSetChanged();
        mPagerAdapter.initFragments();
        mViewPager.setAdapter(mPagerAdapter);
        mIndicator.setViewPager(mViewPager);

        saveWidgetScreen(screen);
        mViewPager.setCurrentItem(mPagerAdapter.getItemCount() - 1);
    }

    private void deleteScreen(WidgetScreen screen) {
        if (mWidgetManager.getMinScreens() >= mWidgetManager.getWidgetScreens().size()) {
            Toast.makeText(this, "Min limit reached", Toast.LENGTH_SHORT).show();
            return;
        }

        mWidgetManager.deleteScreen(screen);
        mWidgetManager.sendToDevice();

        WearableApplication.deviceService(mDevice).requestDeviceInfo();
        mWidgetManager = mDevice.getDeviceCoordinator().getWidgetManager(mDevice);
        mWidgetScreens.clear();
        mWidgetScreens.addAll(mWidgetManager.getWidgetScreens());

        mPagerAdapter.notifyDataSetChanged();
        mPagerAdapter.initFragments();
        mViewPager.setAdapter(mPagerAdapter);
        mIndicator.setViewPager(mViewPager);
    }

    private void updateAvailableWidgets() {
        List<WidgetPart> parts = mWidgetManager.getSupportedWidgetParts(mCurrentPart.getType());
        mWidgetParts.clear();
        mWidgetParts.addAll(parts);
        mWidgetAdapter.notifyDataSetChanged();
    }

    private void saveWidgetScreen(WidgetScreen screen) {
        mWidgetManager.saveScreen(screen);
        mWidgetManager.sendToDevice();

        WearableApplication.deviceService(mDevice).requestDeviceInfo();

        // Get the screen id if it was just created
        // Assume that the screen was just created, hence the last item in the list
        if (screen.getId() == null) {
            mWidgetManager = mDevice.getDeviceCoordinator().getWidgetManager(mDevice);
            mWidgetScreens.clear();
            mWidgetScreens.addAll(mWidgetManager.getWidgetScreens());

            final WidgetScreen lastScreen = mWidgetScreens.get(mWidgetScreens.size() - 1);
            screen.setId(lastScreen.getId());

            mCurrentScreen = screen;
        }

        update();
    }

    private void update() {
        mPagerAdapter.update();
    }

    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.fresh_wearable_widget_settings, menu);
            menu.findItem(R.id.widget_settings_add_screen).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == R.id.widget_settings_add_screen) {
                addScreen();
                return true;
            } else if (item.getItemId() == R.id.widget_settings_rearrange_screens) {
                return true;
            }

            return true;
        }
    };

    private static class FragmentPagerAdapter extends FragmentStateAdapter {
        private List<WidgetScreen> mWidgetScreens;
        private List<WidgetPreviewFragment> mFragments = new ArrayList<>();

        private WatchWidgetPreview.OnWidgetItemClickListener mOnWidgetItemClickListener;

        private WidgetPreviewFragment.OnDeleteItemClickListener mDeleteListener;

        private final int mMinScreens;

        public FragmentPagerAdapter(FragmentActivity fa, List<WidgetScreen> widgetScreens, WatchWidgetPreview.OnWidgetItemClickListener listener, WidgetPreviewFragment.OnDeleteItemClickListener deleteListener, int minScreens) {
            super(fa);
            mWidgetScreens = widgetScreens;
            mOnWidgetItemClickListener = listener;
            mDeleteListener = deleteListener;
            mMinScreens = minScreens;
            initFragments();
        }

        public void initFragments() {
            mFragments.clear();
            for (WidgetScreen screen : mWidgetScreens) {
                mFragments.add(new WidgetPreviewFragment(screen, mOnWidgetItemClickListener, mDeleteListener));
            }
            setDeleteVisibility(mWidgetScreens.size() > mMinScreens);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position >= mWidgetScreens.size()) {
                throw new IllegalArgumentException("Invalid position " + position);
            }

            return mFragments.get(position);
        }

        @Override
        public void onBindViewHolder(@NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
            super.onBindViewHolder(holder, position, payloads);
            setDeleteVisibility(mWidgetScreens.size() > mMinScreens);
        }

        @Override
        public int getItemCount() {
            return mWidgetScreens.size();
        }

        public WidgetPreviewFragment getFragment(int position) {
            return mFragments.get(position);
        }

        public void update() {
            for (WidgetPreviewFragment fragment : mFragments) {
                fragment.update();
                fragment.setDeleteVisibility(mWidgetScreens.size() > mMinScreens);
            };
        }

        public void setDeleteVisibility(boolean visible) {
            Log.d("FragmentPagerAdapter", "Size: " + mWidgetScreens.size());
            for (WidgetPreviewFragment fragment : mFragments) {
                Log.d("FragmentPagerAdapter", "Setting delete visibility to " + visible);
                fragment.setDeleteVisibility(visible);
            }
        }
    }
}
