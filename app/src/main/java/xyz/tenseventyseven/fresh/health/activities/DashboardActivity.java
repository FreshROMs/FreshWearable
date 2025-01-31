package xyz.tenseventyseven.fresh.health.activities;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import dev.oneuiproject.oneui.layout.ToolbarLayout;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.WearableApplication;
import xyz.tenseventyseven.fresh.common.CommonActivityAbstract;
import xyz.tenseventyseven.fresh.databinding.HealthActivityMainBinding;
import xyz.tenseventyseven.fresh.health.activities.dashboard.ProfileFragment;
import xyz.tenseventyseven.fresh.health.activities.dashboard.TogetherFragment;
import xyz.tenseventyseven.fresh.health.activities.dashboard.HomeFragment;
import xyz.tenseventyseven.fresh.health.activities.dashboard.MainFragmentCommon;
import xyz.tenseventyseven.fresh.health.activities.dashboard.FitnessFragment;

public class DashboardActivity extends CommonActivityAbstract {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardActivity.class);

    private HealthActivityMainBinding binding;
    private BottomNavigationView bottomNavigation;
    private ViewPager2 viewPager;
    private MainFragmentsPagerAdapter pagerAdapter;
    private ToolbarLayout toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = HealthActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();

        // Connect to all devices in a Thread
        new Thread(this::initDeviceConnections).start();
    }

    private void setupNavigation() {
        toolbar = binding.healthMainToolbar;
        bottomNavigation = binding.healthActivityMainView;
        bottomNavigation.seslSetGroupDividerEnabled(true);

        // Configure ViewPager2 with fragment adapter and default fragment
        viewPager = binding.healthActivityMainNavHostFragment;
        viewPager.setUserInputEnabled(false);
        pagerAdapter = new MainFragmentsPagerAdapter(this);
        pagerAdapter.setFragmentChangeListener(new FragmentChangeListener() {
            private MainFragmentCommon lastFragment = null;

            @Override
            public void onFragmentChange(MainFragmentCommon fragment) {
                // Ensure the menu provider is set to the current fragment
                if (lastFragment != null) {
                    DashboardActivity.this.removeMenuProvider(lastFragment);
                }

                if (fragment != null) {
                    lastFragment = fragment;
                    toolbar.setTitle(fragment.getTitle());
                    DashboardActivity.this.addMenuProvider(fragment);
                }
            }
        });
        viewPager.setAdapter(pagerAdapter);

        bottomNavigation.setOnItemSelectedListener(menuItem -> {
            final int itemId = menuItem.getItemId();
            if (itemId == R.id.health_navigation_home) {
                viewPager.setCurrentItem(0, false);
            } else if (itemId == R.id.health_navigation_together) {
                viewPager.setCurrentItem(1, false);
            } else if (itemId == R.id.health_navigation_fitness) {
                viewPager.setCurrentItem(2, false);
            } else if (itemId == R.id.health_navigation_profile) {
                viewPager.setCurrentItem(3, false);
            }

            return true;
        });

        viewPager.setCurrentItem(0, false);
    }

    private void initDeviceConnections() {
        WearableApplication.deviceService().requestDeviceInfo();
        DeviceManager deviceManager = WearableApplication.app().getDeviceManager();
        List<GBDevice> devices = deviceManager.getDevices();

        if (!devices.isEmpty()) {
            for (GBDevice device : devices) {
                if (!device.isConnected() && device.getDeviceCoordinator().isHealthTrackingDevice()) {
                    WearableApplication.deviceService(device).connect();
                }
            }
        }
    }

    private interface FragmentChangeListener {
        void onFragmentChange(MainFragmentCommon fragment);
    }

    private class MainFragmentsPagerAdapter extends FragmentStateAdapter {

        private FragmentChangeListener listener;
        private final List<MainFragmentCommon> fragments = new ArrayList<>();
        private int lastPosition = -1;

        public MainFragmentsPagerAdapter(FragmentActivity fa) {
            super(fa);
            initFragments();
        }

        public void setFragmentChangeListener(FragmentChangeListener listener) {
            this.listener = listener;
        }

        private void initFragments() {
            fragments.add(new HomeFragment());
            fragments.add(new TogetherFragment());
            fragments.add(new FitnessFragment());
            fragments.add(new ProfileFragment());
        }

        public MainFragmentCommon getFragment(int position) {
            MainFragmentCommon fragment = null;
            switch (position) {
                case 0:
                    fragment = new HomeFragment();
                    break;
                case 1:
                    fragment = new TogetherFragment();
                    break;
                case 2:
                    fragment = new FitnessFragment();
                    break;
                case 3:
                    fragment = new ProfileFragment();
                    break;
            }

            fragments.set(position, fragment);

            if (listener != null) {
                lastPosition = position;
                listener.onFragmentChange(fragments.get(position));
            }

            return fragment;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return getFragment(position);
        }

        @Override
        public long getItemId(int position) {
            if (listener != null && lastPosition != position) {
                lastPosition = position;
                listener.onFragmentChange(fragments.get(position));
            }

            return super.getItemId(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }

}