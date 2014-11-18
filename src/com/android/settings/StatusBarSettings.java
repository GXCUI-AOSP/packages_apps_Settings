/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.provider.Settings;

public class StatusBarSettings extends SettingsPreferenceFragment
        implements OnSharedPreferenceChangeListener {

    private CheckBoxPreference mShowBatteryPercentage;
    private static final String KEY_SHOW_BATTERY_PERCENTAGE = "show_battery_percent";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.status_bar_prefs);
        initUI();
    }

    private void initUI() {
        mShowBatteryPercentage = (CheckBoxPreference) findPreference(KEY_SHOW_BATTERY_PERCENTAGE);
        boolean showPercentage = Settings.System.getInt(
            getContentResolver(), "status_bar_show_battery_percent", 0) > 0;
        mShowBatteryPercentage.setChecked(showPercentage);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                             .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (key.equals(KEY_SHOW_BATTERY_PERCENTAGE)) {
            boolean showPercentage = preferences.getBoolean(key, true);
            Settings.System.putInt(getContentResolver(), "status_bar_show_battery_percent",
                                   showPercentage ? 1 : 0);
        }
    }
}
