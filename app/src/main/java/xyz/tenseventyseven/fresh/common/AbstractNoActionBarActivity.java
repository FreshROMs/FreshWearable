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

import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;

public abstract class AbstractNoActionBarActivity extends AbstractActivity {
    public static void init(AppActivity activity) {
        int style = R.style.GadgetbridgeTheme_NoActionBar;
        activity.setTheme(style);
        AppCompatDelegate.setDefaultNightMode(Application.isDarkThemeEnabled() ?
                AppCompatDelegate.MODE_NIGHT_YES :
                AppCompatDelegate.MODE_NIGHT_NO
        );
        activity.setLanguage(Application.getLanguage(), false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        init(this);
        super.setupListeners();
        super.onCreate(savedInstanceState);
    }
}
