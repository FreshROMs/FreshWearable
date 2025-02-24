/*  Copyright (C) 2016-2024 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Damien Gaignon, Daniele Gobbetti, José Rebelo, Petr Vaněk, Sebastian Kranz

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
package nodomain.freeyourgadget.gadgetbridge.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import xyz.tenseventyseven.fresh.Application;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * Class holding the common user information needed by most activity trackers
 */
public class ActivityUser {

    public static final int GENDER_FEMALE = 0;
    public static final int GENDER_MALE = 1;
    public static final int GENDER_OTHER = 2;

    private String activityUserName;
    private int activityUserGender;
    private LocalDate activityUserDateOfBirth;
    private int activityUserHeightCm;
    private int activityUserWeightKg;
    private int activityUserSleepDurationGoal;
    private int activityUserStepsGoal;
    private int activityUserCaloriesBurntGoal;
    private int activityUserDistanceGoalMeters;
    private int activityUserActiveTimeGoalMinutes;
    private int activityUserStandingTimeGoalHours;
    private int activityUserStepLengthCm;

    private static final String defaultUserName = "gadgetbridge-user";
    public static final int defaultUserGender = GENDER_FEMALE;
    public static final String defaultUserDateOfBirth = "2000-01-01";
    public static final int defaultUserAge = 0;
    public static final int defaultUserHeightCm = 175;
    public static final int defaultUserWeightKg = 70;
    public static final int defaultUserSleepDurationGoal = 7;
    public static final int defaultUserStepsGoal = 8000;
    public static final int defaultUserCaloriesBurntGoal = 350;
    public static final int defaultUserDistanceGoalMeters = 5000;
    public static final int defaultUserActiveTimeGoalMinutes = 60;
    public static final int defaultUserStepLengthCm = 0;
    public static final int defaultUserGoalWeightKg = 70;
    public static final int defaultUserGoalStandingTimeHours = 12;
    public static final int defaultUserFatBurnTimeMinutes = 30;

    public static final String PREF_USER_NAME = "mi_user_alias";
    public static final String PREF_USER_DATE_OF_BIRTH = "activity_user_date_of_birth";
    public static final String PREF_USER_GENDER = "activity_user_gender";
    public static final String PREF_USER_HEIGHT_CM = "activity_user_height_cm";
    public static final String PREF_USER_WEIGHT_KG = "activity_user_weight_kg";
    public static final String PREF_USER_SLEEP_DURATION = "activity_user_sleep_duration";
    public static final String PREF_USER_STEPS_GOAL = "fitness_goal"; // FIXME: for compatibility
    public static final String PREF_USER_CALORIES_BURNT = "activity_user_calories_burnt";
    public static final String PREF_USER_DISTANCE_METERS = "activity_user_distance_meters";
    public static final String PREF_USER_ACTIVETIME_MINUTES = "activity_user_activetime_minutes";
    public static final String PREF_USER_STEP_LENGTH_CM = "activity_user_step_length_cm";
    public static final String PREF_USER_GOAL_WEIGHT_KG = "activity_user_goal_weight_kg";
    public static final String PREF_USER_GOAL_STANDING_TIME_HOURS = "activity_user_goal_standing_hours";
    public static final String PREF_USER_GOAL_FAT_BURN_TIME_MINUTES = "activity_user_goal_fat_burn_time_minutes";

    public ActivityUser() {
        fetchPreferences();
    }

    public String getName() {
        return activityUserName;
    }

    public int getWeightKg() {
        return activityUserWeightKg;
    }

    /**
     * @see #GENDER_FEMALE
     * @see #GENDER_MALE
     * @see #GENDER_OTHER
     */
    public int getGender() {
        return activityUserGender;
    }

    public LocalDate getDateOfBirth() {
        return activityUserDateOfBirth;
    }

    /**
     * @return the user defined height or a default value when none is set or the stored
     * value is 0.
     */

    public int getHeightCm() {
        if (activityUserHeightCm < 1) {
            activityUserHeightCm = defaultUserHeightCm;
        }
        return activityUserHeightCm;
    }

    /**
     * @return the user defined step length or the calculated default value when none is set or the stored
     * value is 0.
     */
    public int getStepLengthCm() {
        if (activityUserStepLengthCm < 1) {
            activityUserStepLengthCm = (int) (getHeightCm() * 0.43);
        }
        return activityUserStepLengthCm;
    }

    /**
     * @return the user defined sleep duration or the default value when none is set or the stored
     * value is out of any logical bounds.
     */
    public int getSleepDurationGoal() {
        if (activityUserSleepDurationGoal < 1 || activityUserSleepDurationGoal > 24) {
            activityUserSleepDurationGoal = defaultUserSleepDurationGoal;
        }
        return activityUserSleepDurationGoal;
    }

    public int getStepsGoal() {
        if (activityUserStepsGoal < 1) {
            activityUserStepsGoal = defaultUserStepsGoal;
        }
        return activityUserStepsGoal;
    }

    public int getAge() {
        return Period.between(getDateOfBirth(), LocalDate.now()).getYears();
    }

    private void fetchPreferences() {
        Prefs prefs = Application.getPrefs();
        activityUserName = prefs.getString(PREF_USER_NAME, defaultUserName);
        activityUserGender = prefs.getInt(PREF_USER_GENDER, defaultUserGender);
        activityUserHeightCm = prefs.getInt(PREF_USER_HEIGHT_CM, defaultUserHeightCm);
        activityUserWeightKg = prefs.getInt(PREF_USER_WEIGHT_KG, defaultUserWeightKg);
        activityUserDateOfBirth = prefs.getLocalDate(PREF_USER_DATE_OF_BIRTH, defaultUserDateOfBirth);
        activityUserSleepDurationGoal = prefs.getInt(PREF_USER_SLEEP_DURATION, defaultUserSleepDurationGoal);
        activityUserStepsGoal = prefs.getInt(PREF_USER_STEPS_GOAL, defaultUserStepsGoal);
        activityUserCaloriesBurntGoal = prefs.getInt(PREF_USER_CALORIES_BURNT, defaultUserCaloriesBurntGoal);
        activityUserDistanceGoalMeters = prefs.getInt(PREF_USER_DISTANCE_METERS, defaultUserDistanceGoalMeters);
        activityUserActiveTimeGoalMinutes = prefs.getInt(PREF_USER_ACTIVETIME_MINUTES, defaultUserActiveTimeGoalMinutes);
        activityUserStandingTimeGoalHours = prefs.getInt(PREF_USER_GOAL_STANDING_TIME_HOURS, defaultUserGoalStandingTimeHours);
        activityUserStepLengthCm = prefs.getInt(PREF_USER_STEP_LENGTH_CM, defaultUserStepLengthCm);
    }

    /**
     * @deprecated use {@link #getDateOfBirth()}.
     */
    @Deprecated
    public Date getUserBirthday() {
        final LocalDate dateOfBirth = getDateOfBirth();
        Calendar cal = DateTimeUtils.getCalendarUTC();
        cal.set(GregorianCalendar.YEAR, dateOfBirth.getYear());
        cal.set(GregorianCalendar.MONTH, dateOfBirth.getMonthValue() - 1);
        cal.set(GregorianCalendar.DAY_OF_MONTH, dateOfBirth.getDayOfMonth());
        return cal.getTime();
    }

    public int getCaloriesBurntGoal()
    {
        if (activityUserCaloriesBurntGoal < 1) {
            activityUserCaloriesBurntGoal = defaultUserCaloriesBurntGoal;
        }
        return activityUserCaloriesBurntGoal;
    }

    public int getDistanceGoalMeters()
    {
        if (activityUserDistanceGoalMeters < 1) {
            activityUserDistanceGoalMeters = defaultUserDistanceGoalMeters;
        }
        return activityUserDistanceGoalMeters;
    }

    public int getActiveTimeGoalMinutes()
    {
        if (activityUserActiveTimeGoalMinutes < 1) {
            activityUserActiveTimeGoalMinutes = defaultUserActiveTimeGoalMinutes;
        }
        return activityUserActiveTimeGoalMinutes;
    }

    public int getStandingTimeGoalHours()
    {
        if (activityUserStandingTimeGoalHours < 1) {
            activityUserStandingTimeGoalHours = defaultUserGoalStandingTimeHours;
        }
        return activityUserStandingTimeGoalHours;
    }
}
