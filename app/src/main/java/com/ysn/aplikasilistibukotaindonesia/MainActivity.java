package com.ysn.aplikasilistibukotaindonesia;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnClickListener {

    private EditText editTextInputNamaKota;
    private Button buttonCari;
    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private LatLng latLngSelected;

    private List<DataKota> listDataKota;
    private String namaKota;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadComponents();
        initDataKota();
    }

    private void initDataKota() {
        listDataKota = new ArrayList<>();
        listDataKota.add(new DataKota("banda aceh", 5.548290, 95.323756));
        listDataKota.add(new DataKota("medan", 3.595196, 98.672223));
        listDataKota.add(new DataKota("padang", -0.947083, 100.417181));
        listDataKota.add(new DataKota("pekanbaru", 0.507068, 101.447779));
        listDataKota.add(new DataKota("jambi", -1.610123, 103.613120));
        listDataKota.add(new DataKota("palembang", -2.976074, 104.775431));
        listDataKota.add(new DataKota("bengkulu", -3.577847, 102.346387));
        listDataKota.add(new DataKota("bandar lampung", -5.397140, 105.266789));
        listDataKota.add(new DataKota("pangkal pinang", -2.131627, 106.116930));
        listDataKota.add(new DataKota("tanjung pinang", 0.918550, 104.466507));

    }

    private void loadComponents() {
        editTextInputNamaKota = (EditText) findViewById(R.id.edit_text_input_nama_kota_activity_main);
        buttonCari = (Button) findViewById(R.id.button_cari_activity_main);
        buttonCari.setOnClickListener(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_activity_main);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1 * 1000);
        locationRequest.setFastestInterval(1 * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (lastLocation == null) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        } else {
            LatLng myLocationNow = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(myLocationNow)
                    .title("My Location");
            this.googleMap.addMarker(markerOptions);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(myLocationNow));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(myLocationNow)
                    .zoom(16)
                    .build();
            this.googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // memulai google play services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                this.googleMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            this.googleMap.setMyLocationEnabled(true);
        }
    }

    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onClick(View view) {
        if (view == buttonCari) {
            String namaKota = editTextInputNamaKota.getText().toString();
            if (TextUtils.isEmpty(namaKota)) {
                Toast.makeText(MainActivity.this, "Nama kota tidak valid", Toast.LENGTH_SHORT)
                        .show();
            } else {
                boolean kotaAvailable = checkName(namaKota);
                if (kotaAvailable) {
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(latLngSelected)
                            .title(namaKota);
                    googleMap.addMarker(markerOptions);
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(latLngSelected)
                            .zoom(10)
                            .build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                } else {
                    Toast.makeText(MainActivity.this, "Nama kota tidak tersedia", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    private boolean checkName(String namaKota) {
        for (DataKota dataKota : listDataKota) {
            if (dataKota.getNama().equalsIgnoreCase(namaKota)) {
                latLngSelected = new LatLng(dataKota.getLatitude(), dataKota.getLongitude());
                namaKota = dataKota.getNama();
                return true;
            }
        }
        return false;
    }
}