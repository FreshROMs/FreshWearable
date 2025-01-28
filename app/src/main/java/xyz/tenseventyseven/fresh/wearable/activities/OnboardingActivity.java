/*  Copyright (C) 2024 Arjan Schrijver, John Vincent Corcega (TenSeventy7)

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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.activities.welcome.WelcomeFragmentGetStarted;
import xyz.tenseventyseven.fresh.common.CommonActivityAbstract;
import xyz.tenseventyseven.fresh.wearable.activities.onboarding.PermissionsFragment;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.activities.onboarding.IntroFragment;

public class OnboardingActivity extends CommonActivityAbstract {
    private static final Logger LOG = LoggerFactory.getLogger(OnboardingActivity.class);

    private ViewPager2 pager;
    private FragmentPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CommonActivityAbstract.init(this, CommonActivityAbstract.NO_ACTIONBAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Configure ViewPager2 with fragment adapter and default fragment
        pager = findViewById(R.id.welcome_viewpager);
        adapter = new FragmentPagerAdapter(this);

        pager.setAdapter(adapter);
        pager.setUserInputEnabled(false); // Disable swipe
    }

    public void nextFragment() {
        int next = pager.getCurrentItem() + 1;
        if (next < adapter.getItemCount()) {
            pager.setCurrentItem(next, false);
        }
    }

    private static class FragmentPagerAdapter extends FragmentStateAdapter {
        public FragmentPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new IntroFragment();
                case 1:
                    return new PermissionsFragment();
                default:
                    return new WelcomeFragmentGetStarted();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}