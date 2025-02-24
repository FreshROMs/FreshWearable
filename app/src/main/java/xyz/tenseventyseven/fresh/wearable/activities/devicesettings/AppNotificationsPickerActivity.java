package xyz.tenseventyseven.fresh.wearable.activities.devicesettings;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.apppickerview.widget.AppPickerView;
import androidx.core.view.MenuProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.oneuiproject.oneui.layout.ToolbarLayout;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.common.AbstractNoActionBarActivity;
import xyz.tenseventyseven.fresh.databinding.WearActivityAppNotificationsPickerBinding;

public class AppNotificationsPickerActivity extends AbstractNoActionBarActivity implements MenuProvider,
        ToolbarLayout.SearchModeListener {
    private GBDevice device;
    private WearActivityAppNotificationsPickerBinding binding;
    private boolean isBindInitiated = false;
    private String searchQuery = "";
    private AppPickerView.ViewHolder allAppsViewHolder;
    private final ArrayList<String> packagesList = new ArrayList<>();
    private Set<String> blockedPackages;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = WearActivityAppNotificationsPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            Log.e("AppNotificationsPickerActivity", "No device found in intent");
            finish();
            return;
        }

        blockedPackages = Application.getPerDeviceAppsNotifBlackList(device.getAddress());
        if (blockedPackages == null) {
            blockedPackages = new ArraySet<>();
        }

        updateInstalledPackages();
        addMenuProvider(this);
    }

    private void onItemSwitchChanged(CharSequence packageName, boolean isChecked) {
        if (isChecked) {
            blockedPackages.remove(packageName.toString());
            Application.removeFromAppsNotifBlacklistForDevice(device.getAddress(), packageName.toString());
        } else {
            blockedPackages.add(packageName.toString());
            Application.addAppToNotifBlacklistForDevice(device.getAddress(), packageName.toString());
        }

        updateAllAppsSwitch();
    }

    private void setProgressVisibility(boolean visible) {
        binding.apppickerProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateInstalledPackages() {
        setProgressVisibility(true);
        executor.execute(() -> {
            List<ApplicationInfo> installedApplications = getPackageManager().getInstalledApplications(0);
            for (ApplicationInfo appInfo : installedApplications) {
                if (hasLaunchActivity(appInfo) && (!isSystemApp(appInfo) || hasLaunchActivity(appInfo))
                        && appInfo.enabled) {
                    packagesList.add(appInfo.packageName);
                }
            }

            handler.post(() -> {
                binding.appPickerView.setAppPickerView(AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS, packagesList);
                setupAppPickerView();
                setProgressVisibility(false);
            });
        });
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        return (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) > 0;
    }

    private boolean hasLaunchActivity(ApplicationInfo appInfo) {
        return getPackageManager().getLaunchIntentForPackage(appInfo.packageName) != null;
    }

    private void updateAllAppsSwitch() {
        if (binding.toolbarLayout.isSearchMode() && !searchQuery.isEmpty()) {
            return;
        }

        if (allAppsViewHolder != null) {
            SwitchCompat switchCompat = allAppsViewHolder.getSwitch();
            if (switchCompat == null) return;
            switchCompat.setOnCheckedChangeListener(null);
            switchCompat.setChecked(blockedPackages.isEmpty());
            switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
                onSetAllAppsSwitch(isChecked);
            });
        }
    }

    private void onSetAllAppsSwitch(boolean isChecked) {
        if (isChecked) {
            blockedPackages.clear();
        } else {
            blockedPackages.addAll(packagesList);
        }

        Application.setAppsNotifBlackListForDevice(device.getAddress(), blockedPackages);
        binding.appPickerView.refresh();
    }

    private void setupAppPickerView() {
        if (isBindInitiated) return;
        isBindInitiated = true;

        binding.appPickerView.setOnBindListener((view, position, packageName) -> {
            SwitchCompat switchCompat = view.getSwitch();

            boolean isAllApps = packageName.equalsIgnoreCase(AppPickerView.ALL_APPS_STRING);
            if (isAllApps) {
                TextView textView = view.getAppLabel();

                allAppsViewHolder = view;
                if (textView != null) {
                    textView.setText(R.string.wear_app_picker_all_apps);
                }
                updateAllAppsSwitch();
                return;
            }

            if (switchCompat != null) {
                boolean enabled = !blockedPackages.contains(packageName);
                switchCompat.setChecked(enabled);
                switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> onItemSwitchChanged(packageName, isChecked));
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (binding.toolbarLayout.isSearchMode()) {
            binding.toolbarLayout.endSearchMode();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.wear_device_notifications_app_picker, menu);
        menu.findItem(R.id.menu_app_picker_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_app_picker_search) {
            binding.toolbarLayout.startSearchMode(this);
            return true;
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchQuery = query;
        binding.appPickerView.setSearchFilter(searchQuery);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        searchQuery = query;
        binding.appPickerView.setSearchFilter(searchQuery);
        return false;
    }

    @Override
    public void onSearchModeToggle(@NonNull SearchView searchView, boolean toggled) {
        if (!toggled) {
            searchQuery = "";
            binding.appPickerView.setSearchFilter(searchQuery);
            return;
        }

        searchView.setQueryHint(getString(R.string.wear_app_picker_search_hint));
    }
}