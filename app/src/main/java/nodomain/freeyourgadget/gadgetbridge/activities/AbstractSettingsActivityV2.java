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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import dev.oneuiproject.oneui.layout.ToolbarLayout;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.common.CommonActivityAbstract;

public abstract class AbstractSettingsActivityV2 extends CommonActivityAbstract implements
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    protected abstract String fragmentTag();
    protected abstract PreferenceFragmentCompat newFragment();

    private ToolbarLayout toolbarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_sub_settings);
        if (savedInstanceState == null) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag());
            if (fragment == null) {
                fragment = newFragment();
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, fragment, fragmentTag())
                    .commit();

            toolbarLayout = findViewById(R.id.toolbar_layout);
            toolbarLayout.setNavigationButtonAsBack();
            toolbarLayout.setTitle(getString(R.string.action_settings));
        }
    }

    @Override
    public boolean onPreferenceStartScreen(final PreferenceFragmentCompat caller, final PreferenceScreen preferenceScreen) {
        final PreferenceFragmentCompat fragment = newFragment();
        final Bundle args;
        if (fragment.getArguments() != null) {
            args = fragment.getArguments();
        } else {
            args = new Bundle();
        }
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, fragment, preferenceScreen.getKey())
                .addToBackStack(preferenceScreen.getKey())
                .commit();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // Simulate a back press, so that we don't actually exit the activity when
            // in a nested PreferenceScreen
            this.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (toolbarLayout == null) {
            toolbarLayout = findViewById(R.id.toolbar_layout);
        }

        if (toolbarLayout != null) {
            toolbarLayout.setNavigationButtonAsBack();
        }
    }

    public void setActionBarTitle(final CharSequence title) {
        if (toolbarLayout != null) {
            toolbarLayout.setTitle(title);
        }
    }
}
