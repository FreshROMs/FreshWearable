package xyz.tenseventyseven.fresh.wearable.activities;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final List<AppPickerView.ViewHolder> appPickerViewHolders = new ArrayList<>();
    private final Map<String, Boolean> appList = new HashMap<>();

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

        updateInstalledPackages();
        addMenuProvider(this);
    }

    private void onItemSwitchChanged(CharSequence packageName, boolean isChecked) {
        appList.put(packageName.toString(), isChecked);
        if (isChecked) {
            Application.removeFromAppsNotifBlacklistForDevice(device.getAddress(), packageName.toString());
        } else {
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
            ArrayList<String> packagesList = new ArrayList<>();
            for (ApplicationInfo appInfo : installedApplications) {
                if (hasLaunchActivity(appInfo) && (!isSystemApp(appInfo) || hasLaunchActivity(appInfo))
                        && appInfo.enabled) {
                    packagesList.add(appInfo.packageName);
                    appList.put(appInfo.packageName, !Application.isAppBlacklisted(device.getAddress(), appInfo.packageName));
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

        boolean allAppsEnabled = true;
        for (Map.Entry<String, Boolean> entry : appList.entrySet()) {
            if (!entry.getValue()) {
                allAppsEnabled = false;
                break;
            }
        }

        if (!appPickerViewHolders.isEmpty()) {
            AppPickerView.ViewHolder allAppsViewHolder = appPickerViewHolders.get(0);
            SwitchCompat switchCompat = allAppsViewHolder.getSwitch();
            if (switchCompat != null) {
                switchCompat.setOnCheckedChangeListener(null);
                switchCompat.setChecked(allAppsEnabled);
                switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    for (AppPickerView.ViewHolder viewHolder : appPickerViewHolders) {
                        if (viewHolder.getSwitch() != null) {
                            viewHolder.getSwitch().setChecked(isChecked);
                        }
                    }
                });
            }
        }
    }

    private void setupAppPickerView() {
        if (isBindInitiated) return;
        isBindInitiated = true;

        binding.appPickerView.setOnBindListener((view, position, packageName) -> {
            SwitchCompat switchCompat = view.getSwitch();
            TextView textView = view.getAppLabel();

            appPickerViewHolders.add(view);

            if (textView != null) {
                if (position == 0 && !binding.toolbarLayout.isSearchMode()) {
                    textView.setText(R.string.wear_app_picker_all_apps);
                }
            }

            if (switchCompat != null) {
                if (position != 0 || binding.toolbarLayout.isSearchMode()) {
                    boolean enabled = appList.get(packageName) != null && appList.get(packageName);
                    switchCompat.setChecked(enabled);
                    switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> onItemSwitchChanged(packageName, isChecked));

                    updateAllAppsSwitch();
                }
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