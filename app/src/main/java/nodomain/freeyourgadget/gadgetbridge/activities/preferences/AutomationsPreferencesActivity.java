/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.preferences;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2;
import nodomain.freeyourgadget.gadgetbridge.database.PeriodicExporter;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class AutomationsPreferencesActivity extends AbstractSettingsActivityV2 {
    @Override
    protected String fragmentTag() {
        return AutomationsPreferencesFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new AutomationsPreferencesFragment();
    }

    public static class AutomationsPreferencesFragment extends AbstractPreferenceFragment {
        protected static final Logger LOG = LoggerFactory.getLogger(AutomationsPreferencesFragment.class);

        static final String FRAGMENT_TAG = "AUTOMATIONS_PREFERENCES_FRAGMENT";

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.automations_preferences, rootKey);

            final ActivityResultLauncher<String> exportFileChooser = registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("application/x-sqlite3"),
                    uri -> {
                        LOG.info("Got target backup file: {}", uri);
                        if (uri != null) {
                            requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            Application.getPrefs().getPreferences()
                                    .edit()
                                    .putString(GBPrefs.AUTO_EXPORT_LOCATION, uri.toString())
                                    .apply();
                            String summary = getAutoExportLocationSummary(GBPrefs.AUTO_EXPORT_LOCATION);
                            final Preference preference = findPreference(GBPrefs.AUTO_EXPORT_LOCATION);
                            if (preference != null) {
                                preference.setSummary(summary);
                            }
                            final boolean autoExportEnabled = Application.getPrefs().getBoolean(GBPrefs.AUTO_EXPORT_ENABLED, false);
                            final int autoExportPeriod = Application.getPrefs().getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0);
                            PeriodicExporter.scheduleAlarm(requireContext().getApplicationContext(), autoExportPeriod, autoExportEnabled);
                        }
                    }
            );

            final Preference autoExportLocationPref = findPreference(GBPrefs.AUTO_EXPORT_LOCATION);
            if (autoExportLocationPref != null) {
                autoExportLocationPref.setOnPreferenceClickListener(preference -> {
                    exportFileChooser.launch("Gadgetbridge.db");
                    return true;
                });
                autoExportLocationPref.setSummary(getAutoExportLocationSummary(GBPrefs.AUTO_EXPORT_LOCATION));
            }

            final Preference autoExportIntervalPref = findPreference(GBPrefs.AUTO_EXPORT_INTERVAL);
            if (autoExportIntervalPref != null) {
                autoExportIntervalPref.setOnPreferenceChangeListener((preference, autoExportInterval) -> {
                    final String summary = String.format(
                            requireContext().getApplicationContext().getString(R.string.pref_summary_auto_export_interval),
                            Integer.valueOf((String) autoExportInterval)
                    );
                    preference.setSummary(summary);
                    final boolean autoExportEnabled = Application.getPrefs().getBoolean(GBPrefs.AUTO_EXPORT_ENABLED, false);
                    PeriodicExporter.scheduleAlarm(requireContext().getApplicationContext(), Integer.valueOf((String) autoExportInterval), autoExportEnabled);
                    return true;
                });
                final int autoExportInterval = Application.getPrefs().getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0);
                String summary = String.format(
                        requireContext().getApplicationContext().getString(R.string.pref_summary_auto_export_interval),
                        autoExportInterval
                );
                autoExportIntervalPref.setSummary(summary);
            }

            final Preference autoExportEnabledPref = findPreference(GBPrefs.AUTO_EXPORT_ENABLED);
            if (autoExportEnabledPref != null) {
                autoExportEnabledPref.setOnPreferenceChangeListener((preference, autoExportEnabled) -> {
                    int autoExportInterval = Application.getPrefs().getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0);
                    PeriodicExporter.scheduleAlarm(requireContext().getApplicationContext(), autoExportInterval, (boolean) autoExportEnabled);
                    return true;
                });
            }

            final Preference autoFetchIntervalLimitPref = findPreference("auto_fetch_interval_limit");
            if (autoFetchIntervalLimitPref != null) {
                autoFetchIntervalLimitPref.setOnPreferenceChangeListener((preference, autoFetchInterval) -> {
                    final String summary = String.format(
                            requireContext().getApplicationContext().getString(R.string.pref_auto_fetch_limit_fetches_summary),
                            Integer.valueOf((String) autoFetchInterval)
                    );
                    preference.setSummary(summary);
                    return true;
                });

                final int autoFetchInterval = Application.getPrefs().getInt("auto_fetch_interval_limit", 0);
                final String summary = String.format(
                        requireContext().getApplicationContext().getString(R.string.pref_auto_fetch_limit_fetches_summary),
                        autoFetchInterval
                );
                autoFetchIntervalLimitPref.setSummary(summary);
            }
        }

        /**
         * Either returns the file path of the selected document, or the display name, or an empty string
         **/
        public String getAutoExportLocationSummary(final String prefKey) {
            final String autoExportLocation = Application.getPrefs().getString(prefKey, null);
            if (autoExportLocation == null) {
                return "";
            }
            final Uri uri = Uri.parse(autoExportLocation);
            try {
                return AndroidUtils.getFilePath(requireContext().getApplicationContext(), uri);
            } catch (final IllegalArgumentException e1) {
                LOG.warn("Failed to resolve location summary, attempting fallback", e1);
                try (Cursor cursor = requireContext().getContentResolver().query(
                        uri,
                        new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                        null, null, null, null
                )) {
                    if (cursor != null && cursor.moveToFirst()) {
                        return cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                    }
                } catch (final Exception e2) {
                    LOG.error("Failed to resolve location summary in fallback", e2);
                }
            }

            return "";
        }
    }
}
