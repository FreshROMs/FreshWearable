/*  Copyright (C) 2019-2024 Andreas Shimokawa, José Rebelo, keeshii,
    Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import static lineageos.providers.WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT;
import static lineageos.providers.WeatherContract.WeatherColumns.WeatherCode.ISOLATED_THUNDERSHOWERS;
import static lineageos.providers.WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE;
import static lineageos.providers.WeatherContract.WeatherColumns.WeatherCode.SCATTERED_SNOW_SHOWERS;
import static lineageos.providers.WeatherContract.WeatherColumns.WeatherCode.SCATTERED_THUNDERSTORMS;
import static lineageos.providers.WeatherContract.WeatherColumns.WeatherCode.SHOWERS;
import static lineageos.providers.WeatherContract.WeatherColumns.WindSpeedUnit.MPH;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import lineageos.weather.LineageWeatherManager;
import lineageos.weather.WeatherInfo;
import lineageos.weather.WeatherLocation;
import lineageos.weather.util.WeatherUtils;
import xyz.tenseventyseven.fresh.BuildConfig;
import xyz.tenseventyseven.fresh.Application;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.PendingIntentUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

@RequiresApi(api = Build.VERSION_CODES.M)
public class LineageOsWeatherReceiver extends BroadcastReceiver implements LineageWeatherManager.WeatherUpdateRequestListener, LineageWeatherManager.LookupCityRequestListener {

    private static final Logger LOG = LoggerFactory.getLogger(LineageOsWeatherReceiver.class);

    private WeatherLocation weatherLocation = null;
    private Context mContext;
    private PendingIntent mPendingIntent = null;

    public LineageOsWeatherReceiver() {
        mContext = Application.getContext();
        final LineageWeatherManager weatherManager = LineageWeatherManager.getInstance(mContext);
        if (weatherManager == null) {
            return;
        }

        Prefs prefs = Application.getPrefs();

        String city = prefs.getString("weather_city", null);
        String cityId = prefs.getString("weather_cityid", null);

        if ((cityId == null || cityId.equals("")) && city != null && !city.equals("")) {
            lookupCity(city);
        } else if (city != null && cityId != null) {
            weatherLocation = new WeatherLocation.Builder(cityId, city).build();
            enablePeriodicAlarm(true);
        }
    }

    private void lookupCity(String city) {
        final LineageWeatherManager weatherManager = LineageWeatherManager.getInstance(mContext);
        if (weatherManager == null) {
            return;
        }

        if (city != null && !city.equals("")) {
            if (weatherManager.getActiveWeatherServiceProviderLabel() != null) {
                weatherManager.lookupCity(city, this);
            }
        }
    }

    private void enablePeriodicAlarm(boolean enable) {
        if ((mPendingIntent != null && enable) || (mPendingIntent == null && !enable)) {
            return;
        }

        AlarmManager am = (AlarmManager) (mContext.getSystemService(Context.ALARM_SERVICE));
        if (am == null) {
            LOG.warn("could not get alarm manager!");
            return;
        }

        if (enable) {
            Intent intent = new Intent("GB_UPDATE_WEATHER");
            intent.setPackage(BuildConfig.APPLICATION_ID);
            mPendingIntent = PendingIntentUtils.getBroadcast(mContext, 0, intent, 0, false);
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + 10000, AlarmManager.INTERVAL_HOUR, mPendingIntent);
        } else {
            am.cancel(mPendingIntent);
            mPendingIntent = null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Prefs prefs = Application.getPrefs();

        String city = prefs.getString("weather_city", null);
        String cityId = prefs.getString("weather_cityid", null);

        if (city != null && !city.equals("") && cityId == null) {
            lookupCity(city);
        } else {
            requestWeather();
        }
    }

    private void requestWeather() {
        final LineageWeatherManager weatherManager = LineageWeatherManager.getInstance(Application.getContext());
        if (weatherManager.getActiveWeatherServiceProviderLabel() != null && weatherLocation != null) {
            weatherManager.requestWeatherUpdate(weatherLocation, this);
        }
    }

    /**
     * @param cmCondition LineageOS/CyanogenMod condition code
     * @return Yahoo condition code
     */
    private static int LineageOSToYahooCondition(int cmCondition) {
        int yahooCondition;
        if (cmCondition <= SHOWERS) {
            yahooCondition = cmCondition;
        } else if (cmCondition <= SCATTERED_THUNDERSTORMS) {
            yahooCondition = cmCondition + 1;
        } else if (cmCondition <= SCATTERED_SNOW_SHOWERS) {
            yahooCondition = cmCondition + 2;
        } else if (cmCondition <= ISOLATED_THUNDERSHOWERS) {
            yahooCondition = cmCondition + 3;
        } else {
            yahooCondition = NOT_AVAILABLE;
        }
        return yahooCondition;
    }

    @Override
    public void onWeatherRequestCompleted(int status, WeatherInfo weatherInfo) {
        if (weatherInfo != null) {
            LOG.info("weather: " + weatherInfo.toString());
            WeatherSpec weatherSpec = new WeatherSpec();
            weatherSpec.timestamp = (int) (weatherInfo.getTimestamp() / 1000);
            weatherSpec.location = weatherInfo.getCity();

            if (weatherInfo.getTemperatureUnit() == FAHRENHEIT) {
                weatherSpec.currentTemp = (int) WeatherUtils.fahrenheitToCelsius(weatherInfo.getTemperature()) + 273;
                weatherSpec.todayMaxTemp = (int) WeatherUtils.fahrenheitToCelsius(weatherInfo.getTodaysHigh()) + 273;
                weatherSpec.todayMinTemp = (int) WeatherUtils.fahrenheitToCelsius(weatherInfo.getTodaysLow()) + 273;
            } else {
                weatherSpec.currentTemp = (int) weatherInfo.getTemperature() + 273;
                weatherSpec.todayMaxTemp = (int) weatherInfo.getTodaysHigh() + 273;
                weatherSpec.todayMinTemp = (int) weatherInfo.getTodaysLow() + 273;
            }
            if (weatherInfo.getWindSpeedUnit() == MPH) {
                weatherSpec.windSpeed = (float) weatherInfo.getWindSpeed() * 1.609344f;
            } else {
                weatherSpec.windSpeed = (float) weatherInfo.getWindSpeed();
            }
            weatherSpec.windDirection = (int) weatherInfo.getWindDirection();

            weatherSpec.currentConditionCode = Weather.mapToOpenWeatherMapCondition(LineageOSToYahooCondition(weatherInfo.getConditionCode()));
            weatherSpec.currentCondition = Weather.getConditionString(weatherSpec.currentConditionCode);
            weatherSpec.currentHumidity = (int) weatherInfo.getHumidity();

            weatherSpec.forecasts = new ArrayList<>();
            List<WeatherInfo.DayForecast> forecasts = weatherInfo.getForecasts();
            for (int i = 1; i < forecasts.size(); i++) {
                WeatherInfo.DayForecast cmForecast = forecasts.get(i);
                WeatherSpec.Daily gbForecast = new WeatherSpec.Daily();
                if (weatherInfo.getTemperatureUnit() == FAHRENHEIT) {
                    gbForecast.maxTemp = (int) WeatherUtils.fahrenheitToCelsius(cmForecast.getHigh()) + 273;
                    gbForecast.minTemp = (int) WeatherUtils.fahrenheitToCelsius(cmForecast.getLow()) + 273;
                } else {
                    gbForecast.maxTemp = (int) cmForecast.getHigh() + 273;
                    gbForecast.minTemp = (int) cmForecast.getLow() + 273;
                }
                gbForecast.conditionCode = Weather.mapToOpenWeatherMapCondition(LineageOSToYahooCondition(cmForecast.getConditionCode()));
                weatherSpec.forecasts.add(gbForecast);
            }
            ArrayList<WeatherSpec> weatherSpecs = new ArrayList<>(Collections.singletonList(weatherSpec));
            Weather.getInstance().setWeatherSpec(weatherSpecs);
            Application.deviceService().onSendWeather(weatherSpecs);
        } else {
            LOG.info("request has returned null for WeatherInfo");
        }
    }

    @Override
    public void onLookupCityRequestCompleted(int result, List<WeatherLocation> list) {
        if (list != null) {
            weatherLocation = list.get(0);
            String cityId = weatherLocation.getCityId();
            String city = weatherLocation.getCity();

            SharedPreferences.Editor editor = Application.getPrefs().getPreferences().edit();
            editor.putString("weather_city", city).apply();
            editor.putString("weather_cityid", cityId).apply();
            enablePeriodicAlarm(true);
            requestWeather();
        } else {
            enablePeriodicAlarm(false);
        }
    }
}
