/*  Copyright (C) 2017-2024 abettenburg, AndrewBedscastle, Carsten Pfeiffer,
    Daniele Gobbetti, José Rebelo, Petr Vaněk

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
import android.widget.CheckedTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import androidx.recyclerview.widget.RecyclerView;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.NotificationFilterActivity;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static xyz.tenseventyseven.fresh.Application.packageNameToPebbleMsgSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppBlacklistAdapter extends RecyclerView.Adapter<AppBlacklistAdapter.AppBLViewHolder> implements Filterable {
    protected static final Logger LOG = LoggerFactory.getLogger(AppBlacklistAdapter.class);

    public static final String STRING_EXTRA_PACKAGE_NAME = "packageName";

    private final List<ApplicationInfo> applicationInfoList;
    private final int mLayoutId;
    private final Context mContext;
    private final PackageManager mPm;
    private final IdentityHashMap<ApplicationInfo, String> mNameMap;

    private ApplicationFilter applicationFilter;

    public AppBlacklistAdapter(int layoutId, Context context) {
        mLayoutId = layoutId;
        mContext = context;
        mPm = context.getPackageManager();

        applicationInfoList = getAllApplications();

        // sort the package list by label and blacklist status
        mNameMap = new IdentityHashMap<>(applicationInfoList.size());
        for (ApplicationInfo ai : applicationInfoList) {
            CharSequence name = mPm.getApplicationLabel(ai);
            if (name == null) {
                name = ai.packageName;
            }
            if (Application.appIsNotifBlacklisted(ai.packageName) || Application.appIsPebbleBlacklisted(packageNameToPebbleMsgSender(ai.packageName))) {
                // sort blacklisted first by prefixing with a '!'
                name = "!" + name;
            }
            mNameMap.put(ai, name.toString());
        }

        Collections.sort(applicationInfoList, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo ai1, ApplicationInfo ai2) {
                final String s1 = mNameMap.get(ai1);
                final String s2 = mNameMap.get(ai2);
                return s1.compareToIgnoreCase(s2);
            }
        });

    }

    @Override
    public AppBlacklistAdapter.AppBLViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false);
        return new AppBLViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final AppBlacklistAdapter.AppBLViewHolder holder, int position) {
        final ApplicationInfo appInfo = applicationInfoList.get(position);

        holder.deviceAppVersionAuthorLabel.setText(appInfo.packageName);
        holder.deviceAppNameLabel.setText(mNameMap.get(appInfo));
        holder.deviceImageView.setImageDrawable(appInfo.loadIcon(mPm));

        holder.blacklist_checkbox.setChecked(Application.appIsNotifBlacklisted(appInfo.packageName));
        holder.blacklist_pebble_checkbox.setChecked(Application.appIsPebbleBlacklisted(packageNameToPebbleMsgSender(appInfo.packageName)));

        holder.blacklist_pebble_checkbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((CheckedTextView) view).toggle();
                if (((CheckedTextView) view).isChecked()) {
                    Application.addAppToPebbleBlacklist(appInfo.packageName);
                } else {
                    Application.removeFromAppsPebbleBlacklist(appInfo.packageName);
                }

            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckedTextView checkBox = (v.findViewById(R.id.item_checkbox));
                checkBox.toggle();
                if (checkBox.isChecked()) {
                    Application.addAppToNotifBlacklist(appInfo.packageName);
                } else {
                    Application.removeFromAppsNotifBlacklist(appInfo.packageName);
                }
            }
        });

        holder.btnConfigureApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Application.getPrefs().getString("notification_list_is_blacklist", "true").equals("true")) {
                    if (holder.blacklist_checkbox.isChecked()) {
                        GB.toast(mContext, mContext.getString(R.string.toast_app_must_not_be_selected), Toast.LENGTH_SHORT, GB.INFO);
                    } else {
                        Intent intentStartNotificationFilterActivity = new Intent(mContext, NotificationFilterActivity.class);
                        intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_NAME, appInfo.packageName);
                        mContext.startActivity(intentStartNotificationFilterActivity);
                    }
                } else {
                    if (holder.blacklist_checkbox.isChecked()) {
                        Intent intentStartNotificationFilterActivity = new Intent(mContext, NotificationFilterActivity.class);
                        intentStartNotificationFilterActivity.putExtra(STRING_EXTRA_PACKAGE_NAME, appInfo.packageName);
                        mContext.startActivity(intentStartNotificationFilterActivity);
                    } else {
                        GB.toast(mContext, mContext.getString(R.string.toast_app_must_be_selected), Toast.LENGTH_SHORT, GB.INFO);
                    }
                }
            }
        });
    }

    /**
     * Returns all applications on the device, including applications in work profiles.
     */
    public List<ApplicationInfo> getAllApplications() {
        final Set<String> allPackageNames = new HashSet<>();
        final List<ApplicationInfo> ret = new LinkedList<>();

        // Get apps for the current user
        final List<ApplicationInfo> currentUserApps = mPm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (final ApplicationInfo app : currentUserApps) {
            allPackageNames.add(app.packageName);
            ret.add(app);
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
                        allPackageNames.add(app.getApplicationInfo().packageName);
                        ret.add(app.getApplicationInfo());
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed to get apps from other users", e);
        }

        return ret;
    }

    public void checkAllApplications() {
        Set<String> apps_blacklist = new HashSet<>();
        List<ApplicationInfo> allApps = getAllApplications();
        for (ApplicationInfo ai : allApps) {
            apps_blacklist.add(ai.packageName);
        }
        Application.setAppsNotifBlackList(apps_blacklist);
        notifyDataSetChanged();
    }

    public void uncheckAllApplications() {
        Set<String> apps_blacklist = new HashSet<>();
        Application.setAppsNotifBlackList(apps_blacklist);
        notifyDataSetChanged();
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

    class AppBLViewHolder extends RecyclerView.ViewHolder {

        final CheckedTextView blacklist_checkbox;
        final CheckedTextView blacklist_pebble_checkbox;
        final ImageView deviceImageView;
        final TextView deviceAppVersionAuthorLabel;
        final TextView deviceAppNameLabel;
        final ImageView btnConfigureApp;

        AppBLViewHolder(View itemView) {
            super(itemView);

            blacklist_checkbox = itemView.findViewById(R.id.item_checkbox);
            blacklist_pebble_checkbox = itemView.findViewById(R.id.item_pebble_checkbox);
            deviceImageView = itemView.findViewById(R.id.item_image);
            deviceAppVersionAuthorLabel = itemView.findViewById(R.id.item_details);
            deviceAppNameLabel = itemView.findViewById(R.id.item_name);
            btnConfigureApp = itemView.findViewById(R.id.btn_configureApp);
        }

    }

    private class ApplicationFilter extends Filter {

        private final AppBlacklistAdapter adapter;
        private final List<ApplicationInfo> originalList;
        private final List<ApplicationInfo> filteredList;

        private ApplicationFilter(AppBlacklistAdapter adapter, List<ApplicationInfo> originalList) {
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
