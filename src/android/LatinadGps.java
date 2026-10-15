package com.latinad.cordova.plugin.gps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * LatinadGps — reads the position straight from LocationManager's GPS provider.
 *
 * navigator.geolocation in the WebView goes through the platform "fused" provider, which on
 * some devices (Android 10 without Google Play Services, e.g. Taurus TCC160) does not keep the
 * GPS running and stops delivering fixes. Reading GPS_PROVIDER directly avoids that. A last
 * known fix (GPS, then passive) that is recent enough is returned immediately; otherwise one
 * live update is requested until the timeout.
 */
public class LatinadGps extends CordovaPlugin {

    private static final int ERR_PERMISSION = 1;
    private static final int ERR_UNAVAILABLE = 2;
    private static final int ERR_TIMEOUT = 3;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if ("getCurrentPosition".equals(action)) {
            final long timeoutMs = args.optLong(0, 15000);
            final long maxAgeMs = args.optLong(1, 60000);
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getCurrentPosition(timeoutMs, maxAgeMs, callbackContext);
                }
            });
            return true;
        }
        return false;
    }

    private void getCurrentPosition(long timeoutMs, long maxAgeMs, final CallbackContext callbackContext) {
        Context context = cordova.getActivity().getApplicationContext();

        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            sendError(callbackContext, ERR_PERMISSION, "ACCESS_FINE_LOCATION not granted");
            return;
        }

        final LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null || !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            sendError(callbackContext, ERR_UNAVAILABLE, "GPS provider disabled or unavailable");
            return;
        }

        try {
            Location last = freshest(lm.getLastKnownLocation(LocationManager.GPS_PROVIDER),
                    lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
            if (last != null && ageMs(last) <= maxAgeMs) {
                callbackContext.success(toJson(last));
                return;
            }

            final Handler handler = new Handler(Looper.getMainLooper());
            final boolean[] done = {false};
            final LocationListener[] holder = new LocationListener[1];
            final Runnable onTimeout = new Runnable() {
                @Override
                public void run() {
                    if (done[0]) return;
                    done[0] = true;
                    lm.removeUpdates(holder[0]);
                    sendError(callbackContext, ERR_TIMEOUT, "Timed out waiting for a GPS fix");
                }
            };

            holder[0] = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (done[0]) return;
                    done[0] = true;
                    handler.removeCallbacks(onTimeout);
                    lm.removeUpdates(this);
                    try {
                        callbackContext.success(toJson(location));
                    } catch (JSONException e) {
                        sendError(callbackContext, ERR_UNAVAILABLE, String.valueOf(e.getMessage()));
                    }
                }

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
            };

            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, holder[0], Looper.getMainLooper());
            handler.postDelayed(onTimeout, timeoutMs);
        } catch (SecurityException e) {
            sendError(callbackContext, ERR_PERMISSION, e.getMessage());
        } catch (Exception e) {
            sendError(callbackContext, ERR_UNAVAILABLE, String.valueOf(e.getMessage()));
        }
    }

    private static Location freshest(Location a, Location b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.getElapsedRealtimeNanos() >= b.getElapsedRealtimeNanos() ? a : b;
    }

    private static long ageMs(Location l) {
        return (SystemClock.elapsedRealtimeNanos() - l.getElapsedRealtimeNanos()) / 1000000L;
    }

    private static JSONObject toJson(Location l) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("lat", l.getLatitude());
        o.put("lon", l.getLongitude());
        o.put("accuracy", l.getAccuracy());
        o.put("altitude", l.hasAltitude() ? l.getAltitude() : JSONObject.NULL);
        o.put("provider", l.getProvider());
        o.put("ageMs", ageMs(l));
        return o;
    }

    private static void sendError(CallbackContext cb, int code, String message) {
        JSONObject o = new JSONObject();
        try {
            o.put("code", code);
            o.put("message", message);
        } catch (JSONException ignored) {
        }
        cb.error(o);
    }
}
