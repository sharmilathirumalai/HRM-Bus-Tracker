package com.example.hrmbustracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/*
  Background Task to fetch data from GFTS using protocol buffer and update the current location.
 */


public class VehiclePositionReader  extends AsyncTask<Object, Void, List<FeedEntity>> {

    Map<String, Marker>   vehicleList;
    private GoogleMap  map = null;
    Context mContext;
    private static String TAG = "Position Reader - Background Thread";

    public VehiclePositionReader(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    protected List<FeedEntity> doInBackground(Object...params ) {
        URL url = null;
        map = (GoogleMap) params[0];
        vehicleList = (Map<String, Marker>) params[1];

        try {
            url = new URL("http://gtfs.halifax.ca/realtime/Vehicle/VehiclePositions.pb");
            List<FeedEntity> feeds;
            return GtfsRealtime.FeedMessage.parseFrom (url.openStream()).getEntityList();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(List<FeedEntity> feedEntities) {
        Log.e(TAG, "map ready");

        /*
            FeedEntities might be empty if there is a connection error.
            So null is checked to prevent app from crash.
        */
        if(feedEntities == null) {
            refreshLocations();
            return;
        }

        // Iterates all fetched entities and does desired actions.

        for (FeedEntity entity : feedEntities) {
            if(entity.getVehicle().hasPosition() && entity.getVehicle().hasTrip()) {
                Double lat = Double.valueOf(entity.getVehicle().getPosition().getLatitude());
                Double lon = Double.valueOf(entity.getVehicle().getPosition().getLongitude());
                String route = entity.getVehicle().getTrip().getRouteId();
                String id = entity.getVehicle().getTrip().getTripId().concat("_" + route);
                Marker m = vehicleList.get(id);

                String busStr = MapsActivity.busStr.toLowerCase();

                //Checks whether user has selected any particular buses to view.

                if(!busStr.isEmpty()) {
                    List<String> busList = Arrays.asList(busStr.split(","));

                    if (busList.size() != 0) {
                        if (!busList.contains(route.toLowerCase())) {
                            if (m != null) {
                                m.remove();
                                vehicleList.remove(id);
                            }
                            continue;
                        }
                    }
                }


                if( m != null) {

                    // Updates already existing Marker
                    MarkerAnimation.animateMarker(new LatLng(lat,lon), m);

                } else {


                    /*
                        Adds new custom marker.
                        @reference: https://stackoverflow.com/questions/15331983/multiple-info-windows-in-android-maps-api-2

                     */

                    LayoutInflater inflater = (LayoutInflater) mContext
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View v = inflater.inflate(R.layout.marker, null);
                    TextView  text = (TextView) v.findViewById(R.id.routenumber);
                    text.setText(route);
                    m = map.addMarker(new MarkerOptions().position(new LatLng(lat,lon)));
                    m.setIcon(BitmapDescriptorFactory.fromBitmap(loadBitmapFromView(v)));
                }

                vehicleList.put(id , m);
            }
        }

        // Callback to refresh the bus state.
        refreshLocations();

    }

    protected void refreshLocations() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new VehiclePositionReader(mContext).execute(map, vehicleList);
            }
        }, 800);
    }


    public static Bitmap loadBitmapFromView(View v) {
        if (v.getMeasuredHeight() <= 0) {
            v.measure(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
            v.draw(c);
            return b;
        }
        return null;
    }
}
