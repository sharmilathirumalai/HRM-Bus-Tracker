package com.example.hrmbustracker;

import android.animation.ValueAnimator;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/*
    Animates the buses movement when the location changes by animating the marker movement.
    Uses Custom Interpolator to animate both rotation and position (Lat,Long) change.
    @reference: https://codinginfinite.com/android-example-animate-marker-map-current-location/
 */

public class MarkerAnimation {
    public static void animateMarker(final LatLng destination, final Marker marker) {
        if (marker != null) {
            final LatLng startPosition = marker.getPosition();
            final LatLng endPosition = new LatLng(destination.latitude, destination.longitude);

            final float startRotation = marker.getRotation();

            final CustomInterpolator latLngInterpolator = new CustomInterpolator();
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);

            // 0.5 sec is set as the duration of movement.
            valueAnimator.setDuration(500);

            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator animation) {
                    try {
                        float v = animation.getAnimatedFraction();
                        LatLng newPosition = latLngInterpolator.interpolate(v, startPosition, endPosition);
                        marker.setPosition(newPosition);
                    } catch (Exception e) {
                        Log.e("Exception: %s", e.getMessage());
                    }
                }
            });

            valueAnimator.start();
        }
    }
}
