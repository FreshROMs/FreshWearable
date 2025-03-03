/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import androidx.recyclerview.widget.RecyclerView;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.app_specific_notifications.AppSpecificNotificationSettingsDetailActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

import static xyz.tenseventyseven.fresh.Application.packageNameToPebbleMsgSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppSpecificNotificationSettingsAppListAdapter extends RecyclerView.Adapter<AppSpecificNotificationSettingsAppListAdapter.AppNotificationSettingsViewHolder> implements Filterable {
    protected static final Logger LOG = LoggerFactory.getLogger(AppSpecificNotificationSettingsAppListAdapter.class);

    public static final String STRING_EXTRA_PACKAGE_NAME = "packageName";
    public static final String STRING_EXTRA_PACKAGE_TITLE = "packageTitle";

    private final List<ApplicationInfo> applicationInfoList;
    private final int mLayoutId;
    private final Context mContext;
    private final PackageManager mPm;
    private GBDevice mDevice;
    private final IdentityHashMap<ApplicationInfo, String> mNameMap;

    private ApplicationFilter applicationFilter;

    public AppSpecificNotificationSettingsAppListAdapter(int layoutId, Context context, GBDevice device) {
        mLayoutId = layoutId;
        mContext = context;
        mPm = context.getPackageManager();
        mDevice = device;

        applicationInfoList = getAllApplications();


        // sort the package list by label and blacklist status
        mNameMap = new IdentityHashMap<>(applicationInfoList.size());
        for (ApplicationInfo ai : applicationInfoList) {
            CharSequence name = mPm.getApplicationLabel(ai);
            if (name == null) {
                name = ai.packageName;
            }
            mNameMap.put(ai, name.toString());
        }

        Collections.sort(applicationInfoList, (ai1, ai2) -> {
            final String s1 = mNameMap.get(ai1);
            final String s2 = mNameMap.get(ai2);
            return s1.compareToIgnoreCase(s2);
        });

    }

    @Override
    public AppNotificationSettingsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false);
        return new AppNotificationSettingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final AppNotificationSettingsViewHolder holder, int position) {
        final ApplicationInfo appInfo = applicationInfoList.get(position);

        holder.deviceAppVersionAuthorLabel.setText(appInfo.packageName);
        holder.deviceAppNameLabel.setText(mNameMap.get(appInfo));
        holder.deviceImageView.setImageDrawable(appInfo.loadIcon(mPm));

        holder.itemView.setOnClickListener(v -> {
            Intent intentStartNotificationFilterActivity = new Intent(mContext, AppSpecificNotificationSettingsDetailActivity.class);
            intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_NAME, appInfo.packageName);
            intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_TITLE, mNameMap.get(appInfo));
            intentStartNotificationFilterActivity.putExtra(GBDevice.EXTRA_DEVICE, mDevice);
            mContext.startActivity(intentStartNotificationFilterActivity);
        });

        holder.btnConfigureApp.setOnClickListener(view -> {
            Intent intentStartNotificationFilterActivity = new Intent(mContext, AppSpecificNotificationSettingsDetailActivity.class);
            intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_NAME, appInfo.packageName);
            intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_TITLE, mNameMap.get(appInfo));
            intentStartNotificationFilterActivity.putExtra(GBDevice.EXTRA_DEVICE, mDevice);
            mContext.startActivity(intentStartNotificationFilterActivity);
        });
    }

    /**
     * Returns the applications for which the Gadgetbridge notifications are enabled.
     */
    public List<ApplicationInfo> getAllApplications() {
        final Set<String> allPackageNames = new HashSet<>();
        final List<ApplicationInfo> ret = new LinkedList<>();
        boolean filterInverted = !Application.getPrefs().getString("notification_list_is_blacklist", "true").equals("true");

        // Get apps for the current user
        final List<ApplicationInfo> currentUserApps = mPm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (final ApplicationInfo app : currentUserApps) {
            boolean blacklisted = Application.appIsNotifBlacklisted(app.packageName) || Application.appIsPebbleBlacklisted(packageNameToPebbleMsgSender(app.packageName));
            if((!filterInverted && !blacklisted) || (filterInverted && blacklisted)) {
                allPackageNames.add(app.packageName);
                ret.add(app);
            }
        }

        // Add all apps from other users (eg. manager profile)
        try {
            final UserHandle currentUser = Process.myUserHandle();
            final LauncherApps launcher = (LauncherApps) mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
            final List<UserHandle> userProfiles = um.getUserProfiles();
            for (final UserHandle userProfile : userProfiles) {
                if (userProfile.equals(currentUser)) {
                    continue;
                }

                final List<LauncherActivityInfo> userActivityList = launcher.getActivityList(null, userProfile);

                for (final LauncherActivityInfo app : userActivityList) {
                    if (!allPackageNames.contains(app.getApplicationInfo().packageName)) {
                        boolean blacklisted = Application.appIsNotifBlacklisted(app.getApplicationInfo().packageName) || Application.appIsPebbleBlacklisted(packageNameToPebbleMsgSender(app.getApplicationInfo().packageName));
                        if((!filterInverted && !blacklisted) || (filterInverted && blacklisted)) {
                            allPackageNames.add(app.getApplicationInfo().packageName);
                            ret.add(app.getApplicationInfo());
                        }
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed to get apps from other users", e);
        }

        return ret;
    }


    @Override
    public int getItemCount() {
        return applicationInfoList.size();
    }

    @Override
    public Filter getFilter() {
        if (applicationFilter == null)
            applicationFilter = new ApplicationFilter(this, applicationInfoList);
        return applicationFilter;
    }

    class AppNotificationSettingsViewHolder extends RecyclerView.ViewHolder {

        final ImageView deviceImageView;
        final TextView deviceAppVersionAuthorLabel;
        final TextView deviceAppNameLabel;
        final ImageView btnConfigureApp;

        AppNotificationSettingsViewHolder(View itemView) {
            super(itemView);

            deviceImageView = itemView.findViewById(R.id.item_image);
            deviceAppVersionAuthorLabel = itemView.findViewById(R.id.item_details);
            deviceAppNameLabel = itemView.findViewById(R.id.item_name);
            btnConfigureApp = itemView.findViewById(R.id.btn_configureApp);
        }

    }

    private class ApplicationFilter extends Filter {

        private final AppSpecificNotificationSettingsAppListAdapter adapter;
        private final List<ApplicationInfo> originalList;
        private final List<ApplicationInfo> filteredList;

        private ApplicationFilter(AppSpecificNotificationSettingsAppListAdapter adapter, List<ApplicationInfo> originalList) {
            super();
            this.originalList = new ArrayList<>(originalList);
            this.filteredList = new ArrayList<>();
            this.adapter = adapter;
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence filter) {
            filteredList.clear();
            final Filter.FilterResults results = new Filter.FilterResults();

            if (filter == null || filter.length() == 0)
                filteredList.addAll(originalList);
            else {
                final String filterPattern = filter.toString().toLowerCase().trim();

                for (ApplicationInfo ai : originalList) {
                    CharSequence name = mPm.getApplicationLabel(ai);
                    if (name.toString().toLowerCase().contains(filterPattern) ||
                            (ai.packageName.contains(filterPattern))) {
                        filteredList.add(ai);
                    }
                }
            }

            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            adapter.applicationInfoList.clear();
            adapter.applicationInfoList.addAll((List<ApplicationInfo>) filterResults.values);
            adapter.notifyDataSetChanged();
        }
    }

}
