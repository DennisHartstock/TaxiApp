package com.example.taxiapp;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.example.taxiapp.databinding.ActivityDriverMapsBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityDriverMapsBinding binding;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseUser currentUser;

    private Button settingsButton, signOutButton;

    private static final int CHECK_SETTINGS_CODE = 111;
    private static final int REQUEST_LOCATION_PERMISSION = 222;

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private boolean isLocationUpdatesActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDriverMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://taxi-app-d2353-default-rtdb.europe-west1.firebasedatabase.app/");
        currentUser = auth.getCurrentUser();

        settingsButton = findViewById(R.id.settingsButton);
        signOutButton = findViewById(R.id.signOutButton);

        signOutButton.setOnClickListener(view -> {
            auth.signOut();
            signOutDriver();
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        buildLocationRequest();
        buildLocationCallback();
        buildLocationSettingsRequest();

        startLocationUpdates();
    }

    private void signOutDriver() {
        String driverId = currentUser.getUid();
        DatabaseReference drivers = FirebaseDatabase.getInstance().getReference().child("drivers");

        GeoFire geoFire = new GeoFire(drivers);
        geoFire.removeLocation(driverId);

        Intent intent = new Intent(DriverMapsActivity.this, ChooseModeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (currentLocation != null) {
            LatLng driverLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(driverLocation).title("Driver Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(driverLocation));
        }

    }

    private void stopLocationUpdates() {
        if (!isLocationUpdatesActive) {
            return;
        }

        fusedLocationClient.removeLocationUpdates(locationCallback).addOnCompleteListener(task -> {
            isLocationUpdatesActive = false;
        });
    }

    private void startLocationUpdates() {
        isLocationUpdatesActive = true;

        settingsClient.checkLocationSettings(locationSettingsRequest).addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                if (ActivityCompat.checkSelfPermission(DriverMapsActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(DriverMapsActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                DriverMapsActivity.this.updateLocationUI();
            }
        })
                .addOnFailureListener(this, e -> {
                    int statusCode = ((ApiException) e).getStatusCode();
                    switch (statusCode) {
                        case LocationSettingsStatusCodes
                                .RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(DriverMapsActivity.this, CHECK_SETTINGS_CODE);
                            } catch (IntentSender.SendIntentException sendIntentException) {
                                sendIntentException.printStackTrace();
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            String message = "Adjust location settings on your device";
                            Toast.makeText(DriverMapsActivity.this, message, Toast.LENGTH_LONG).show();
                            isLocationUpdatesActive = false;
                    }
                    updateLocationUI();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHECK_SETTINGS_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    Log.d("MainActivity", "User has agreed to change location settings");
                    startLocationUpdates();
                    break;
                case Activity.RESULT_CANCELED:
                    Log.d("MainActivity", "User has not agreed to change location settings");
                    isLocationUpdatesActive = false;
                    updateLocationUI();
                    break;
            }
        }
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                updateLocationUI();
            }
        };
    }

    private void updateLocationUI() {
        if (currentLocation != null) {
            LatLng driverLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(driverLocation));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
            mMap.addMarker(new MarkerOptions().position(driverLocation).title("Driver Location"));

            String driverId = currentUser.getUid();
            DatabaseReference drivers = FirebaseDatabase.getInstance().getReference().child("drivers");

            GeoFire geoFire = new GeoFire(drivers);
            geoFire.setLocation(driverId, new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));
        }
    }

    private void buildLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationUpdatesActive && checkLocationPermission()) {
            startLocationUpdates();
        } else if (!checkLocationPermission()) {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            showSnackBar("Location permission is needed for app functionality",
                    "OK",
                    view -> ActivityCompat.requestPermissions(DriverMapsActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION));
        } else {
            ActivityCompat.requestPermissions(DriverMapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    private void showSnackBar(final String mainText, final String action, View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE).setAction(action, listener).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length <= 0) {
                Log.d("onRequestPermissionsResult", "Request was canceled");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdatesActive) {
                    startLocationUpdates();
                }
            } else {
                showSnackBar("Turn on location on settings", "Settings", view -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                });
            }
        }
    }

    private boolean checkLocationPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}