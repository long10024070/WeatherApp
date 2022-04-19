package com.example.weatherapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private Button refreshBtn;
    public TextView textView;
    private ApiCaller weatherapi = new ApiCaller();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        refreshBtn = findViewById(R.id.button);
        refreshBtn.setText("REFRESH");
        textView = findViewById(R.id.textView);

        refresh();
    }

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.

                }
            });

    public void refresh() {
        if (weatherapi.pending) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                textView.setText("Cần phải cấp quyền sử dụng vị trí để xem thông tin thời tiết");
                return;
            }
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        textView.setText("Loading");
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            double lon = location.getLongitude();
                            double lat = location.getLatitude();
                            weatherapi.getWeather(lat, lon);
                            Thread thread = new Thread() {
                                public void run() {
                                    while (weatherapi.pending) {}
                                    String ShowData = "";
                                    try {
                                        JSONObject jsonObj = new JSONObject(weatherapi.data);
                                        ShowData += "\nLongitude: " + String.valueOf(jsonObj.getJSONObject("coord").getDouble("lon"));
                                        ShowData += "\nLatitude: " + String.valueOf(jsonObj.getJSONObject("coord").getDouble("lat"));
                                        ShowData += "\nWeather: " + jsonObj.getJSONArray("weather").getJSONObject(0).getString("description");
                                        ShowData += "\nTemperature: " + jsonObj.getJSONObject("main").getString("temp");
                                        textView.setText(ShowData);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            };
                            thread.start();
                        }
                    }
                });
    }

    public void refreshBtn(View view) {
        refresh();
    }

    private class ApiCaller {
        private static final String url = "https://api.openweathermap.org/data/2.5/weather";
        private static final String appid = "b332bec43ae5c63684d76a73c43d718b";
        public String data = "";
        public Boolean pending = false;

        public void getWeather(double lat, double lon) {
            if (pending) return;
            pending = true;
            String requestlink = url + "?" + "lat=" + String.valueOf(lat) + "&lon=" + String.valueOf(lon) + "&appid=" + appid;
            StringRequest request = new StringRequest(Request.Method.GET, requestlink,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            data = response;
                            pending = false;
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            data = "";
                            pending = false;
                        }
                    });
            VolleySingleton.getInstance(MainActivity.this).addToRequestQueue(request);
        }
    }
}