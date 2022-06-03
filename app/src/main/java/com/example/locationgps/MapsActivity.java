package com.example.locationgps;

import android.location.Location;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.FragmentActivity;

import com.example.locationgps.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    public ListView lv_address;
    String[] lv_address_date = new String[] {"我家","學校","工作場所","籃球場","新天地","7-11","7-11","7-11","7-11","7-11","7-11"};
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    List<Location> savedLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gps_main);
        /*
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        */

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);

        MyApplication myApplication = (MyApplication) getApplicationContext();
        savedLocations = myApplication.getMyLocation();
        lv_address = findViewById(R.id.lv_address);
        ArrayAdapter<String> lv_address_adapter = new ArrayAdapter<String>(this , android.R.layout.simple_expandable_list_item_1,lv_address_date);
        lv_address.setAdapter(lv_address_adapter);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        //LatLng lastLocationPlaced = new LatLng(24.1252214, 120.6744177);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        LatLng lastLocationPlaced = new LatLng(24.1252214, 120.6744177);
        for (Location location:savedLocations){
            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Lat:"+location.getLatitude()+"\t"+"Lon:"+location.getLongitude());
            mMap.addMarker(markerOptions);
            lastLocationPlaced = latLng;
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLocationPlaced,17.0f));
        setMyLocation();
    }

    //  顯示定位圖層
    private void setMyLocation() throws SecurityException {
        mMap.setMyLocationEnabled(true); // 顯示定位圖層
    }
}