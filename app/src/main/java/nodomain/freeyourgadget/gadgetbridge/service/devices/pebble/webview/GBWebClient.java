/*  Copyright (C) 2017-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Pavel Elagin

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SunriseTransitSet;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import xyz.tenseventyseven.fresh.Application;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.util.WebViewSingleton;

public class GBWebClient extends WebViewClient {

    private String[] AllowedDomains = new String[]{
            "openweathermap.org",   //for weather :)
            "rawgit.com",           //for trekvolle
            "tagesschau.de"         //for internal watchapp tests
    };
    private static final Logger LOG = LoggerFactory.getLogger(GBWebClient.class);

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        Uri parsedUri = request.getUrl();
        LOG.debug("WEBVIEW shouldInterceptRequest URL: " + parsedUri.toString());
        WebResourceResponse mimickedReply = mimicReply(parsedUri);
        if (mimickedReply != null)
            return mimickedReply;
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        LOG.debug("WEBVIEW shouldInterceptRequest URL (legacy): " + url);
        Uri parsedUri = Uri.parse(url);
        WebResourceResponse mimickedReply = mimicReply(parsedUri);
        if (mimickedReply != null)
            return mimickedReply;
        return super.shouldInterceptRequest(view, url);
    }


    private WebResourceResponse mimicReply(Uri requestedUri) {
        if (requestedUri.getHost() != null && (StringUtils.indexOfAny(requestedUri.getHost(), AllowedDomains) != -1)) {
            if (WebViewSingleton.getInstance().internetHelperBound) {
                LOG.debug("WEBVIEW forwarding request to the internet helper");
                Bundle bundle = new Bundle();
                bundle.putString("URL", requestedUri.toString());
                Message webRequest = Message.obtain();
                webRequest.setData(bundle);
                try {
                    return WebViewSingleton.getInstance().send(webRequest);
                } catch (RemoteException | InterruptedException e) {
                    LOG.warn("Error downloading data from " + requestedUri, e);
                }

            } else {
                if (StringUtils.endsWith(requestedUri.getHost(), "openweathermap.org")){
                    LOG.debug("WEBVIEW request to openweathermap.org detected of type: " + requestedUri.getPath() + " params: " + requestedUri.getQuery());
                    return mimicOpenWeatherMapResponse(requestedUri.getPath(), requestedUri.getQueryParameter("units"));
                } else if (StringUtils.endsWith(requestedUri.getHost(), "rawgit.com")) {
                    LOG.debug("WEBVIEW request to rawgit.com detected of type: " + requestedUri.getPath() + " params: " + requestedUri.getQuery());
                    return mimicRawGitResponse(requestedUri.getPath());
                } else {
                    LOG.debug("WEBVIEW request to allowed domain detected but not intercepted: " + requestedUri.toString());
                }
            }
        } else {
            LOG.debug("WEBVIEW request:" + requestedUri.toString() + " not intercepted");
        }
        return null;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri parsedUri = Uri.parse(url);

        if (parsedUri.getScheme().startsWith("http")) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            Application.getContext().startActivity(i);
        } else if (parsedUri.getScheme().startsWith("pebblejs")) {
            url = url.replaceFirst("^pebblejs://close#", "file:///android_asset/app_config/configure.html?config=true&json=");
            view.loadUrl(url);
        } else if (parsedUri.getScheme().equals("data")) { //clay
            view.loadUrl(url);
        } else {
            LOG.debug("WEBVIEW Ignoring unhandled scheme: " + parsedUri.getScheme());
        }

        return true;
    }

    private WebResourceResponse mimicRawGitResponse(String path) {
        if("/aHcVolle/TrekVolle/master/online.html".equals(path)) { //TrekVolle online check
            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");
            return new WebResourceResponse("text/html", "utf-8", 200, "OK",
                    headers,
                    new ByteArrayInputStream("1".getBytes())
            );
        }

        return null;
    }

    private WebResourceResponse mimicOpenWeatherMapResponse(String type, String units) {

        if (Weather.getInstance() == null) {
            LOG.warn("WEBVIEW - Weather instance is null, cannot update weather");
            return null;
        }

        CurrentPosition currentPosition = new CurrentPosition();

        try {
            JSONObject resp = Weather.getInstance().createReconstructedOWMWeatherReply();
            if ("/data/2.5/weather".equals(type) && resp != null) {
                JSONObject main = resp.getJSONObject("main");

                convertTemps(main, units); //caller might want different units
                JSONObject wind = resp.getJSONObject("wind");
                convertSpeeds(wind, units);

                resp.put("cod", 200);
                resp.put("coord", coordObject(currentPosition));
                resp.put("sys", sysObject(currentPosition));
//            } else if ("/data/2.5/forecast".equals(type) && Weather.getInstance().getWeather2().reconstructedOWMForecast != null) { //this is wrong, as we only have daily data. Unfortunately it looks like daily forecasts cannot be reconstructed
//                resp = new JSONObject(Weather.getInstance().getWeather2().reconstructedOWMForecast.toString());
//
//                JSONObject city = resp.getJSONObject("city");
//                city.put("coord", coordObject(currentPosition));
//
//                JSONArray list = resp.getJSONArray("list");
//                for (int i = 0, size = list.length(); i < size; i++) {
//                    JSONObject item = list.getJSONObject(i);
//                    JSONObject main = item.getJSONObject("main");
//                    convertTemps(main, units); //caller might want different units
//                }
//
//                resp.put("cod", 200);
            } else {
                LOG.warn("WEBVIEW - cannot mimick request of type " + type + " (unsupported or lack of data)");
                return null;
            }

            LOG.info("WEBVIEW - mimic openweather response" + resp.toString());
            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");

            return new WebResourceResponse("application/json", "utf-8", 200, "OK",
                    headers,
                    new ByteArrayInputStream(resp.toString().getBytes())
            );
        } catch (JSONException e) {
            LOG.warn("Error building the JSON weather message.", e);
        }

        return null;

    }


    private static JSONObject sysObject(CurrentPosition currentPosition) throws JSONException {
        final SunriseTransitSet sunriseTransitSet = SPA.calculateSunriseTransitSet(
                ZonedDateTime.now(),
                currentPosition.getLatitude(),
                currentPosition.getLongitude(),
                DeltaT.estimate(LocalDate.now())
        );

        JSONObject sys = new JSONObject();
        sys.put("country", "World");
        sys.put("sunrise", sunriseTransitSet.getSunrise().toInstant().getEpochSecond());
        sys.put("sunset", sunriseTransitSet.getSunset().toInstant().getEpochSecond());

        return sys;
    }

    private static void convertSpeeds(JSONObject wind, String units) throws JSONException {
        if ("metric".equals(units)) {
            wind.put("speed", (wind.getDouble("speed") * 3.6f) );
        } else if ("imperial".equals(units)) { //it's 2018... this is so sad
            wind.put("speed", (wind.getDouble("speed") * 2.237f) );
        }
    }

    private static void convertTemps(JSONObject main, String units) throws JSONException {
        if ("metric".equals(units)) {
            main.put("temp", (int) main.get("temp") - 273);
            main.put("temp_min", (int) main.get("temp_min") - 273);
            main.put("temp_max", (int) main.get("temp_max") - 273);
        } else if ("imperial".equals(units)) { //it's 2017... this is so sad
            main.put("temp", ((int) (main.get("temp")) - 273.15f) * 1.8f + 32);         // lgtm [java/integer-multiplication-cast-to-long]
            main.put("temp_min", ((int) (main.get("temp_min")) - 273.15f) * 1.8f + 32); // lgtm [java/integer-multiplication-cast-to-long]
            main.put("temp_max", ((int) (main.get("temp_max")) - 273.15f) * 1.8f + 32); // lgtm [java/integer-multiplication-cast-to-long]
        }
    }

    private static JSONObject coordObject(CurrentPosition currentPosition) throws JSONException {
        JSONObject coord = new JSONObject();
        coord.put("lat", currentPosition.getLatitude());
        coord.put("lon", currentPosition.getLongitude());
        return coord;
    }

}
