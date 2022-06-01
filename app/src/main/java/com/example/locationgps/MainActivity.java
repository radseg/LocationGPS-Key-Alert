package com.example.locationgps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FAST_UPDATE_INTERVAL = 5;
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address,tv_wayPointCounts;
    Switch sw_locationsupdates, sw_gps;
    FusedLocationProviderClient fusedLocationProviderClient;
    Button goto_user_interface,btn_newWaypoint,btn_showWayPointList,btn_showMap;
    boolean updateOn = false;
    Location currentLocation;
    List<Location> savedLocations;

    LocationRequest locationRequest;
    LocationCallback locationCallBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        sw_gps = findViewById(R.id.sw_gps);
        sw_locationsupdates = findViewById(R.id.sw_locationsupdates);
        goto_user_interface = findViewById(R.id.button2);
        btn_newWaypoint = findViewById(R.id.btn_newWayPoint);
        btn_showWayPointList = findViewById(R.id.btn_showWayPointList);
        tv_wayPointCounts = findViewById(R.id.tv_countOfCrumbs);
        btn_showMap = findViewById(R.id.btn_showMap);

        locationRequest = LocationRequest.create()
                .setInterval(100)
                .setFastestInterval(3000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(100);
        locationRequest.setInterval(300 * DEFAULT_UPDATE_INTERVAL);//更新間隔
        locationRequest.setFastestInterval(1000 * FAST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //save the location
                Location location = locationResult.getLastLocation();
                updateUIValues(location);

            }
        };



        btn_newWaypoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication myApplication = (MyApplication)getApplication();
                savedLocations = myApplication.getMyLocation();
                savedLocations.add(currentLocation);

            }
        });


        btn_showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, MapsActivity.class);
                startActivity(intent);

            }
        });


        btn_showWayPointList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ShowSavedLocationsList.class);
                startActivity(intent);
            }
        });


        sw_gps.setOnClickListener(v -> {
            if (sw_gps.isChecked()) {
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                tv_sensor.setText("Using GPS sensors");
            } else {
                locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                tv_sensor.setText("Using Towers WIFI");
            }
        });

        sw_locationsupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_locationsupdates.isChecked()) {
                    //turn on location tracking
                    startLocationUpdates();
                } else {
                    //turn off location tracking
                    stopLocationUpdates();
                }
            }
        });


        updateGPS();

    }


    private void startLocationUpdates() {

        tv_updates.setText("Location is being tracked");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();
    }

    private void stopLocationUpdates() {

        tv_updates.setText("Location is not being tracked");
        tv_lat.setText("Location is not being tracked");
        tv_lon.setText("Location is not being tracked");
        tv_speed.setText("Location is not being tracked");
        tv_address.setText("Location is not being tracked");
        tv_accuracy.setText("Location is not being tracked");
        tv_altitude.setText("Location is not being tracked");
        tv_sensor.setText("Location is not being tracked");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }






    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    updateGPS();
                }else{
                    Toast.makeText(this,"This app requests permission to be granted in order to work properly",Toast.LENGTH_LONG).show();
                }
        }


    }

    private void updateGPS(){
        //拿到GPS權限，並更新畫面顯示擁有權限
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            //從使用者那邊拿到權限
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    updateUIValues(location);
                    currentLocation = location;
                }
            });
        }else{
            //使用者沒給權限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            //檢查手機版本是不是6.0以上
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_FINE_LOCATION);
            }

        }
    }

    private void updateUIValues(Location location) {
        //取得新GPS地點並更新所有的TextView值
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        if (location.hasAltitude()){
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        }else {
            tv_altitude.setText("Not available");
        }

        if (location.hasSpeed()){
            tv_speed.setText(String.valueOf(location.getSpeed()));
        }else {
            tv_speed.setText("Not available");
        }

        Geocoder geocoder = new Geocoder(MainActivity.this);
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            tv_address.setText(addresses.get(0).getAddressLine(0));
        }catch (Exception e){
            tv_address.setText("Unable to get street address");

        }

        MyApplication myApplication = (MyApplication)getApplication();
        savedLocations = myApplication.getMyLocation();

        tv_wayPointCounts.setText(Integer.toString(savedLocations.size()));
    }


    public void Goto_user_interface(View view){
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, gps_main.class);
        startActivity(intent);
    }
}