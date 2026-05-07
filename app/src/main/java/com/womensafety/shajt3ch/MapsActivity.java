package com.womensafety.shajt3ch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    double latitude, longitude;

    private final int PROXIMITY_RADIUS = 10000;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            checkLocationPermission();

        if (!checkGooglePlayServices()) {
            finish();
            return;
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    private boolean checkGooglePlayServices() {
        GoogleApiAvailability api =
                GoogleApiAvailability.getInstance();

        int result =
                api.isGooglePlayServicesAvailable(this);

        if (result != ConnectionResult.SUCCESS) {
            if (api.isUserResolvableError(result))
                api.getErrorDialog(this, result, 0).show();

            return false;
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        Button btnPolice = findViewById(R.id.btnPolice);
        btnPolice.setOnClickListener(v -> {
            mMap.clear();
            String url = getUrl(latitude, longitude, "police");
            if (url.isEmpty()) {
                Toast.makeText(this, "Missing Google Places API key configuration.", Toast.LENGTH_LONG).show();
                return;
            }

            Object[] data = new Object[]{mMap, url};
            new GetNearbyPlacesData().execute(data);

            Toast.makeText(this,
                    "Nearby Police Stations",
                    Toast.LENGTH_LONG).show();
        });

        Button btnHospital = findViewById(R.id.btnHospital);
        btnHospital.setOnClickListener(v -> {

            mMap.clear();
            String url = getUrl(latitude, longitude, "hospital");
            if (url.isEmpty()) {
                Toast.makeText(this, "Missing Google Places API key configuration.", Toast.LENGTH_LONG).show();
                return;
            }

            Object[] data = new Object[]{mMap, url};
            new GetNearbyPlacesData().execute(data);

            Toast.makeText(this,
                    "Nearby Hospitals",
                    Toast.LENGTH_LONG).show();
        });
    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient =
                new GoogleApiClient.Builder(this)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();

        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi
                    .requestLocationUpdates(
                            mGoogleApiClient,
                            mLocationRequest,
                            this);
        }
    }

    private String getUrl(double lat,
                          double lng,
                          String place) {
        String placesApiKey = BuildConfig.GOOGLE_PLACES_API_KEY;
        if (placesApiKey == null || placesApiKey.trim().isEmpty()) {
            Log.e("MapsActivity", "GOOGLE_PLACES_API_KEY is missing. Please configure it in .env.");
            return "";
        }

        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json?"
                + "location=" + lat + "," + lng
                + "&radius=" + PROXIMITY_RADIUS
                + "&type=" + place
                + "&sensor=true"
                + "&key=" + placesApiKey;
    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;

        if (mCurrLocationMarker != null)
            mCurrLocationMarker.remove();

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        LatLng latLng =
                new LatLng(latitude, longitude);

        MarkerOptions markerOptions =
                new MarkerOptions()
                        .position(latLng)
                        .title("Current Position")
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(
                                        BitmapDescriptorFactory.HUE_MAGENTA));

        mCurrLocationMarker =
                mMap.addMarker(markerOptions);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(
                CameraUpdateFactory.zoomTo(11));

        if (mGoogleApiClient != null)
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(
                            mGoogleApiClient,
                            this);
    }

    @Override public void onConnectionSuspended(int i) {}
    @Override public void onConnectionFailed(ConnectionResult r) {}

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);

            return false;
        }
        return true;
    }
}