package com.example.hrmbustracker;

import com.google.android.gms.maps.model.LatLng;

/*
    Calculates the interpolated Latitude and Longitude According to fraction in timing.
 */

public class CustomInterpolator {

    public LatLng interpolate(float fraction, LatLng a, LatLng b) {
        double temp = b.longitude - a.longitude;
        if (Math.abs(temp) > 180) {
            temp = temp -  Math.signum(temp) * 360;
        }
        double longitude = temp * fraction + a.longitude;
        return new LatLng((b.latitude - a.latitude) * fraction + a.latitude, longitude);
    }
}
