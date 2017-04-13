package com.ysn.aplikasilistibukotaindonesia;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.android.gms.maps.model.PolylineOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RouteMapsActivitiy extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnClickListener {

    private EditText editTextKotaAsal;
    private EditText editTextKotaTujuan;
    private Button buttonCari;
    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private List<DataKota> listDataKota;
    private List<LatLng> markerPoints = new ArrayList<>();
    private List<String> listCitySelected = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_maps);
        EventBus.getDefault().register(this);
        loadComponents();
    }

    private void loadComponents() {
        editTextKotaAsal = (EditText) findViewById(R.id.edit_text_input_nama_kota_asal_activity_route_maps);
        editTextKotaTujuan = (EditText) findViewById(R.id.edit_text_input_nama_kota_tujuan_activity_route_maps);
        buttonCari = (Button) findViewById(R.id.button_cari_activity_route_maps);
        buttonCari.setOnClickListener(this);

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_activity_route_maps);
        supportMapFragment.getMapAsync(this);
    }

    @Subscribe(sticky = true)
    public void onMessageEvent(List<DataKota> listDataKota) {
        if (listDataKota.size() > 0) {
            if (listDataKota.get(0) instanceof DataKota) {
                this.listDataKota = listDataKota;
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        setMyLocationNow();
    }

    private void setMyLocationNow() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1 * 1000);
        locationRequest.setFastestInterval(1 * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        Log.d("RouteMaps", "setMyLocationNow");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location == null) {
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        } else {
            LatLng myLocationNow = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(myLocationNow)
                    .title("My Location");
            googleMap.addMarker(markerOptions);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(myLocationNow));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(myLocationNow)
                    .zoom(16)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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

        //  memulai google play service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                this.googleMap.setMyLocationEnabled(true);
            } else {
                buildGoogleApiClient();
                this.googleMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            this.googleMap.setMyLocationEnabled(true);
        }
        // setMyLocationNow();
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
            //  do something
            String namaKotaAsal = editTextKotaAsal.getText().toString();
            String namaKotaTujuan = editTextKotaTujuan.getText().toString();
            if (TextUtils.isEmpty(namaKotaAsal)) {
                Toast.makeText(RouteMapsActivitiy.this, "Nama kota asal tidak valid", Toast.LENGTH_SHORT)
                        .show();
            } else if (TextUtils.isEmpty(namaKotaTujuan)) {
                Toast.makeText(RouteMapsActivitiy.this, "Nama kota tujuan tidak valid", Toast.LENGTH_SHORT)
                        .show();
            } else {
                //   do something
                if (markerPoints.size() > 1) {
                    markerPoints.clear();
                }
                googleMap.clear();
                boolean namaKotaAsalAvailable = checkNameCity(namaKotaAsal);
                boolean namaKotaTujuanAvailable = checkNameCity(namaKotaTujuan);
                if (namaKotaAsalAvailable || namaKotaTujuanAvailable) {
                    String strOrigin = listCitySelected.get(0);
                    String strDestination = listCitySelected.get(1);

                    // get URL to the Google Directions API
                    // https://maps.googleapis.com/maps/api/directions/json?origin=Medan&destination=Padang&key=AIzaSyCWel6yfkJ_PMYql_REc60Aikc6beLIYAA
                    String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + strOrigin + "&destination=" + strDestination + "&key=AIzaSyCWel6yfkJ_PMYql_REc60Aikc6beLIYAA";

                    FetchUrl fetchUrl = new FetchUrl();
                    fetchUrl.execute(url);

                    // move map camera
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(markerPoints.get(0)));
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(10));

                }

                /*PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions = polylineOptions.add(new LatLng(3.595196, 98.672223));
                polylineOptions = polylineOptions.add(new LatLng(3.595196, 99.672223));
                polylineOptions = polylineOptions.color(Color.RED);
                googleMap.addPolyline(polylineOptions);*/

            }
        }
    }

    private String downloadUrl(String strUrl) throws Exception {
        String data = "";
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // connecting to url
            urlConnection.connect();

            // reading data from url
            inputStream = urlConnection.getInputStream();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuffer stringBuffer = new StringBuffer();

            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            data = stringBuffer.toString();
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            inputStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String data = "";

            try {
                // fecting the data from web service
                data = downloadUrl(strings[0]);
                Log.d("RouteMaps", "doInBackground: " + strings[0] + " data: " + data);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            ParserTask parserTask = new ParserTask();

            // invokes the thread for parsing the JSOn data
            Log.d("RouteMaps", "onPostExecute: " + s);
            parserTask.execute(s);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jsonObject = new JSONObject(strings[0]);
                DataParser parser = new DataParser();

                // start parsing data
                routes = parser.parse(jsonObject);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            super.onPostExecute(lists);
            ArrayList<LatLng> points;
            PolylineOptions polylineOptions = null;
            Log.d("RouteMaps", "lists size: " + lists.size());

            // traversing through all the routes
            for(int i = 0; i < lists.size(); i++) {
                points = new ArrayList<>();
                polylineOptions = new PolylineOptions();

                // fetching i-th route
                List<HashMap<String, String>> path = lists.get(i);

                // fetching all the points in i-th route
                for(int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }

                // adding all the points in the route to LineOptions
                polylineOptions = polylineOptions.addAll(points);
                polylineOptions = polylineOptions.width(5);
                polylineOptions = polylineOptions.color(Color.RED);
                Log.d("RouteMaps", "onPostExecuted polylineOptions decoded");
            }

            // drawing polyline in the Google Map for the i-th route
            if (polylineOptions != null) {
                googleMap.addPolyline(polylineOptions);
                Log.d("RouteMaps", "addPolyline");
            } else {
                // nothing to do in here
            }
        }
    }

    private boolean checkNameCity(String namaKota) {
        for (DataKota dataKota : listDataKota) {
            if (namaKota.equalsIgnoreCase(dataKota.getNama())) {
                LatLng position = new LatLng(dataKota.getLatitude(), dataKota.getLongitude());
                markerPoints.add(position);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title(namaKota);
                listCitySelected.add(namaKota);
                googleMap.addMarker(markerOptions);
                return true;
            }
        }
        return false;
    }

}
