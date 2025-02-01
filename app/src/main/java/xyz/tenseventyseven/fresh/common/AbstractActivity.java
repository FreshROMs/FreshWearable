/*  Copyright (C) 2017-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniele Gobbetti, Petr Vaněk

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
package xyz.tenseventyseven.fresh.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;

public abstract class AbstractActivity extends AppCompatActivity implements AppActivity {
    private boolean isLanguageInvalid = false;

    public static final int NONE = 0;
    public static final int NO_ACTIONBAR = 1;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case Application.ACTION_LANGUAGE_CHANGE:
                    setLanguage(Application.getLanguage(), true);
                    break;
                case Application.ACTION_THEME_CHANGE:
                    getDelegate().applyDayNight();
                    recreate();
                    break;
                case Application.ACTION_QUIT:
                    finish();
                    break;
            }
        }
    };

    public void setLanguage(Locale language, boolean invalidateLanguage) {
        if (invalidateLanguage) {
            isLanguageInvalid = true;
        }
        AndroidUtils.setLanguage(this, language);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupListeners() {
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(Application.ACTION_QUIT);
        filterLocal.addAction(Application.ACTION_LANGUAGE_CHANGE);
        filterLocal.addAction(Application.ACTION_THEME_CHANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLanguageInvalid) {
            isLanguageInvalid = false;
            recreate();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
