/*  Copyright (C) 2018-2024 Andreas Shimokawa, Damien Gaignon, jhey, José
    Rebelo, Maxime Reyrolle, Nephiel, odavo32nof, Petr Vaněk, Raghd Hamzeh,
    sedy89, Stefan Bora, thermatk, xaos, Yoran Vulker, Zhong Jianxin

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class HuamiConst {
    // observed the following values so far:
    // 00 01 02 09 0a 0b 0c 10 11

    // 0 = same activity kind as before
    // 1 = light activity walking?
    // 3 = definitely non-wear
    // 9 = probably light sleep, definitely some kind of sleep
    // 10 = ignore, except for hr (if valid)
    // 11 = probably deep sleep
    // 12 = definitely wake up
    // 17 = definitely not sleep related

    public static final int TYPE_UNSET = -1;
    public static final int TYPE_NO_CHANGE = 0;
    public static final int TYPE_ACTIVITY = 1;
    public static final int TYPE_RUNNING = 2;
    public static final int TYPE_NONWEAR = 3;
    public static final int TYPE_RIDE_BIKE = 4;
    public static final int TYPE_CHARGING = 6;
    public static final int TYPE_LIGHT_SLEEP = 9;
    public static final int TYPE_IGNORE = 10;
    public static final int TYPE_DEEP_SLEEP = 11;
    public static final int TYPE_WAKE_UP = 12;


    public static final String MI_BAND2_NAME = "MI Band 2";
    public static final String MI_BAND2_NAME_HRX = "Mi Band HRX";
    public static final String MI_BAND3_NAME = "Mi Band 3";
    public static final String MI_BAND3_NAME_2 = "Xiaomi Band 3";
    public static final String MI_BAND4_NAME = "Mi Smart Band 4";
    public static final String MI_BAND5_NAME = "Mi Smart Band 5";
    public static final String MI_BAND6_NAME = "Mi Smart Band 6";
    public static final String AMAZFIT_ACTIVE_NAME = "Amazfit Active";
    public static final String AMAZFIT_ACTIVE_2_NAME = "Amazfit Active 2";
    public static final String AMAZFIT_ACTIVE_EDGE_NAME = "Amazfit Active Edge";
    public static final String AMAZFIT_BALANCE_NAME = "Amazfit Balance";
    public static final String AMAZFIT_BAND5_NAME = "Amazfit Band 5";
    public static final String AMAZFIT_BAND7_NAME = "Amazfit Band 7";
    public static final String AMAZFIT_NEO_NAME = "Amazfit Neo";
    public static final String AMAZFIT_X = "Amazfit X";
    public static final String AMAZFIT_BIP5_NAME = "Amazfit Bip 5";
    public static final String AMAZFIT_BIP5_UNITY_NAME = "Amazfit Bip 5 Unity";
    public static final String AMAZFIT_GTS3_NAME = "Amazfit GTS 3";
    public static final String AMAZFIT_GTS4_NAME = "Amazfit GTS 4";
    public static final String AMAZFIT_GTS4_MINI_NAME = "Amazfit GTS 4 Mini";
    public static final String AMAZFIT_GTR3_NAME = "Amazfit GTR 3";
    public static final String AMAZFIT_GTR3_PRO_NAME = "Amazfit GTR 3 Pro";
    public static final String AMAZFIT_GTR4_NAME = "Amazfit GTR 4";
    public static final String AMAZFIT_GTR_MINI_NAME = "Amazfit GTR Mini";
    public static final String AMAZFIT_TREX_2_NAME = "Amazfit T-Rex 2";
    public static final String AMAZFIT_TREX_3_NAME = "Amazfit T-Rex 3";
    public static final String AMAZFIT_TREX_ULTRA = "Amazfit T-Rex Ultra";
    public static final String AMAZFIT_CHEETAH_PRO_NAME = "Amazfit Cheetah Pro";
    public static final String AMAZFIT_CHEETAH_SQUARE_NAME = "Amazfit Cheetah S";
    public static final String AMAZFIT_CHEETAH_ROUND_NAME = "Amazfit Cheetah R";
    public static final String AMAZFIT_FALCON_NAME = "Amazfit Falcon";

    public static final String XIAOMI_SMART_BAND7_NAME = "Xiaomi Smart Band 7";
    public static final String XIAOMI_SMART_BAND7_PRO_NAME = "Xiaomi Smart Band 7 Pro";

    public static final String PREF_DISPLAY_ITEMS = "display_items";
    public static final String PREF_DISPLAY_ITEMS_SORTABLE = "display_items_sortable";
    public static final String PREF_WORKOUT_ACTIVITY_TYPES_SORTABLE = "workout_activity_types_sortable";
    public static final String PREF_SHORTCUTS = "shortcuts";
    public static final String PREF_SHORTCUTS_SORTABLE = "shortcuts_sortable";
    public static final String PREF_CONTROL_CENTER_SORTABLE = "control_center_sortable";
    public static final String PREF_EXPOSE_HR_THIRDPARTY = "expose_hr_thirdparty";
    public static final String PREF_USE_CUSTOM_FONT = "use_custom_font";

    public static final String PREF_BUTTON_ACTION_ENABLE = "button_action_enable";
    public static final String PREF_BUTTON_ACTION_VIBRATE = "button_action_vibrate";
    public static final String PREF_BUTTON_ACTION_PRESS_COUNT = "button_action_press_count";
    public static final String PREF_BUTTON_ACTION_PRESS_MAX_INTERVAL = "button_action_press_max_interval";
    public static final String PREF_BUTTON_ACTION_BROADCAST_DELAY = "button_action_broadcast_delay";
    public static final String PREF_BUTTON_ACTION_BROADCAST = "button_action_broadcast";
    public static final String PREF_BUTTON_ACTION_SELECTION_OFF = "UNKNOWN";
    public static final String PREF_BUTTON_ACTION_SELECTION_BROADCAST = "BROADCAST";
    public static final String PREF_BUTTON_ACTION_SELECTION_FITNESS_APP_START = "FITNESS_CONTROL_START";
    public static final String PREF_BUTTON_ACTION_SELECTION_FITNESS_APP_STOP = "FITNESS_CONTROL_STOP";
    public static final String PREF_BUTTON_ACTION_SELECTION_FITNESS_APP_TOGGLE = "FITNESS_CONTROL_TOGGLE";

    /**
     * The suffixes match the enum {@link nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiVibrationPatternNotificationType}.
     */
    public static final String PREF_HUAMI_VIBRATION_PROFILE_KEY_PREFIX = "vibration_profile_key_";
    public static final String PREF_HUAMI_DEFAULT_VIBRATION_PROFILE = "default";
    // profile
    public static final String PREF_HUAMI_VIBRATION_PROFILE_PREFIX = "huami_vibration_profile_";
    public static final String PREF_HUAMI_VIBRATION_PROFILE_APP_ALERTS = PREF_HUAMI_VIBRATION_PROFILE_PREFIX + "app_alerts";
    public static final String PREF_HUAMI_VIBRATION_PROFILE_INCOMING_CALL = PREF_HUAMI_VIBRATION_PROFILE_PREFIX + "incoming_call";
    public static final String PREF_HUAMI_VIBRATION_PROFILE_INCOMING_SMS = PREF_HUAMI_VIBRATION_PROFILE_PREFIX + "incoming_sms";
    public static final String PREF_HUAMI_VIBRATION_PROFILE_GOAL_NOTIFICATION = PREF_HUAMI_VIBRATION_PROFILE_PREFIX + "goal_notification";
    public static final String PREF_HUAMI_VIBRATION_PROFILE_ALARM = PREF_HUAMI_VIBRATION_PROFILE_PREFIX + "alarm";
    public static final String PREF_HUAMI_VIBRATION_PROFILE_IDLE_ALERTS = PREF_HUAMI_VIBRATION_PROFILE_PREFIX + "idle_alerts";
    public static final String PREF_HUAMI_VIBRATION_PROFILE_EVENT_REMINDER = PREF_HUAMI_VIBRATION_PROFILE_PREFIX + "event_reminder";
    public static final String PREF_HUAMI_VIBRATION_PROFILE_FIND_BAND = PREF_HUAMI_VIBRATION_PROFILE_PREFIX + "find_band";
    public static final String PREF_HUAMI_VIBRATION_PROFILE_TODO_LIST = PREF_HUAMI_VIBRATION_PROFILE_PREFIX + "todo_list";
    public static final String PREF_HUAMI_VIBRATION_PROFILE_SCHEDULE = PREF_HUAMI_VIBRATION_PROFILE_PREFIX + "schedule";
    // count
    public static final String PREF_HUAMI_VIBRATION_COUNT_PREFIX = "huami_vibration_count_";
    public static final String PREF_HUAMI_VIBRATION_COUNT_APP_ALERTS = PREF_HUAMI_VIBRATION_COUNT_PREFIX + "app_alerts";
    public static final String PREF_HUAMI_VIBRATION_COUNT_INCOMING_CALL = PREF_HUAMI_VIBRATION_COUNT_PREFIX + "incoming_call";
    public static final String PREF_HUAMI_VIBRATION_COUNT_INCOMING_SMS = PREF_HUAMI_VIBRATION_COUNT_PREFIX + "incoming_sms";
    public static final String PREF_HUAMI_VIBRATION_COUNT_GOAL_NOTIFICATION = PREF_HUAMI_VIBRATION_COUNT_PREFIX + "goal_notification";
    public static final String PREF_HUAMI_VIBRATION_COUNT_ALARM = PREF_HUAMI_VIBRATION_COUNT_PREFIX + "alarm";
    public static final String PREF_HUAMI_VIBRATION_COUNT_IDLE_ALERTS = PREF_HUAMI_VIBRATION_COUNT_PREFIX + "idle_alerts";
    public static final String PREF_HUAMI_VIBRATION_COUNT_EVENT_REMINDER = PREF_HUAMI_VIBRATION_COUNT_PREFIX + "event_reminder";
    public static final String PREF_HUAMI_VIBRATION_COUNT_FIND_BAND = PREF_HUAMI_VIBRATION_COUNT_PREFIX + "find_band";
    public static final String PREF_HUAMI_VIBRATION_COUNT_TODO_LIST = PREF_HUAMI_VIBRATION_COUNT_PREFIX + "todo_list";
    public static final String PREF_HUAMI_VIBRATION_COUNT_SCHEDULE = PREF_HUAMI_VIBRATION_COUNT_PREFIX + "schedule";
    // try
    public static final String PREF_HUAMI_VIBRATION_TRY_PREFIX = "huami_vibration_try_";
    public static final String PREF_HUAMI_VIBRATION_TRY_APP_ALERTS = PREF_HUAMI_VIBRATION_TRY_PREFIX + "app_alerts";
    public static final String PREF_HUAMI_VIBRATION_TRY_INCOMING_CALL = PREF_HUAMI_VIBRATION_TRY_PREFIX + "incoming_call";
    public static final String PREF_HUAMI_VIBRATION_TRY_INCOMING_SMS = PREF_HUAMI_VIBRATION_TRY_PREFIX + "incoming_sms";
    public static final String PREF_HUAMI_VIBRATION_TRY_GOAL_NOTIFICATION = PREF_HUAMI_VIBRATION_TRY_PREFIX + "goal_notification";
    public static final String PREF_HUAMI_VIBRATION_TRY_ALARM = PREF_HUAMI_VIBRATION_TRY_PREFIX + "alarm";
    public static final String PREF_HUAMI_VIBRATION_TRY_IDLE_ALERTS = PREF_HUAMI_VIBRATION_TRY_PREFIX + "idle_alerts";
    public static final String PREF_HUAMI_VIBRATION_TRY_EVENT_REMINDER = PREF_HUAMI_VIBRATION_TRY_PREFIX + "event_reminder";
    public static final String PREF_HUAMI_VIBRATION_TRY_FIND_BAND = PREF_HUAMI_VIBRATION_TRY_PREFIX + "find_band";
    public static final String PREF_HUAMI_VIBRATION_TRY_TODO_LIST = PREF_HUAMI_VIBRATION_TRY_PREFIX + "todo_list";
    public static final String PREF_HUAMI_VIBRATION_TRY_SCHEDULE = PREF_HUAMI_VIBRATION_TRY_PREFIX + "schedule";

    public static ActivityKind toActivityKind(int rawType) {
        switch (rawType) {
            case TYPE_DEEP_SLEEP:
                return ActivityKind.DEEP_SLEEP;
            case TYPE_LIGHT_SLEEP:
                return ActivityKind.LIGHT_SLEEP;
            case TYPE_ACTIVITY:
            case TYPE_RUNNING:
            case TYPE_WAKE_UP:
                return ActivityKind.ACTIVITY;
            case TYPE_NONWEAR:
                return ActivityKind.NOT_WORN;
            case TYPE_CHARGING:
                return ActivityKind.NOT_WORN; //I believe it's a safe assumption
            case TYPE_RIDE_BIKE:
                return ActivityKind.CYCLING;
            default:
            case TYPE_UNSET: // fall through
                return ActivityKind.UNKNOWN;
        }
    }

    public static int toRawActivityType(ActivityKind activityKind) {
        switch (activityKind) {
            case ACTIVITY:
                return TYPE_ACTIVITY;
            case DEEP_SLEEP:
                return TYPE_DEEP_SLEEP;
            case LIGHT_SLEEP:
                return TYPE_LIGHT_SLEEP;
            case NOT_WORN:
                return TYPE_NONWEAR;
            case UNKNOWN: // fall through
            default:
                return TYPE_UNSET;
        }
    }

}
