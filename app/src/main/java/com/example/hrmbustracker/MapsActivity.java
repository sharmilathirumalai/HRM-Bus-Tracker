package com.example.hrmbustracker;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int LOCATION_REFRESH_TIME = 1;
    private static final int LOCATION_REFRESH_DISTANCE = 1;
    private static final float DEFAULT_ZOOM = 15f;
    private final String SAVED_MAP_STATE = "map_state_camera";
    private Boolean stateRestored = false;
    public static String busStr = "";
    private static String TAG = "Bus Tracking - Main Activity";

    private boolean mLocationPermissionGranted;
    public String bestProvider;

    public static GoogleMap mMap;
    private LatLng currentLocation;
    private Criteria criteria;
    private LocationManager locationManager;
    private EditText buses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /*
            Textfield where selected buses can be entered.
            OnEditorListener is added to get the selected bus when OnDONE Keyboard event is fired.
        */
        buses = (EditText) findViewById(R.id.buses);
        buses.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    busStr = buses.getText().toString();
                }
                return false;
            }
        });

        Log.e(TAG, "create");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "pause");
        /* Save Map state */
        if (mMap != null) {
            MapStateManager manager = new MapStateManager(this);
            manager.saveMapState(mMap);
            stateRestored = false;
        }
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "resume");
        super.onResume();
        setupMap();
    }

    /* Creates Map instance if not present */
    private void setupMap() {
        if (mMap == null) {
            Log.e(TAG, "map setup");
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }


    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (getApplicationContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    getDeviceLocation();
                }
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Dalhousie.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.e(TAG, "map ready");

        if (!mLocationPermissionGranted) {
            getLocationPermission();

        }

        /* Restores Map Camera Position from previous state */
        MapStateManager manager = new MapStateManager(this);
        CameraPosition position = manager.getSavedCameraPosition();

        if (position != null) {
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(position);
            mMap.moveCamera(update);
            mMap.setMapType(manager.getSavedMapType());
            stateRestored = true;
        }
        getDeviceLocation();
        new VehiclePositionReader(getApplicationContext()).execute(mMap, new HashMap<String, Marker>());
    }

    private void getDeviceLocation() {

        /* Checks whether location permission is provided and requests access if not present */
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager = (LocationManager) getSystemService(getApplicationContext().LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();
        double longitude = -63.5917;
        double latitude = 44.6366;
        Location location = null;

        if (currentLocation == null) {
            location = locationManager.getLastKnownLocation(bestProvider);

            if (location != null) {
                Log.e("TAG", "GPS is on");
                longitude = location.getLongitude();
                latitude = location.getLatitude();
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                        LOCATION_REFRESH_DISTANCE, locationListener);
            }
        } else {
            latitude = currentLocation.latitude;
            longitude = currentLocation.longitude;
        }


        // Add a marker to current location and move the camera
        LatLng currentLoc = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(currentLoc).title("Current Location")).showInfoWindow();
        if (!stateRestored) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, DEFAULT_ZOOM));
        }
        currentLocation = currentLoc;

    }

    /* Update User current Location on Change */
    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            stateRestored = true;
            getDeviceLocation();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            getDeviceLocation();
        }

        @Override
        public void onProviderEnabled(String provider) {
            getDeviceLocation();
        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    /* Recenter w.r.t current User Location */
    public void reCenter(View v) {
        stateRestored = false;
        getDeviceLocation();

    }

    /* Set buses to filter */
    public void filterBuses(View v) {
        busStr = buses.getText().toString();
    }

}
