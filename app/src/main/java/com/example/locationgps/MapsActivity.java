package com.example.locationgps;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.example.locationgps.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    public static final String TAG = "MapsActivity";
    public ListView lv_address;
    public EditText ed_searchAddress;
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
                .findFragmentById(R.id.gps_map);
        mapFragment.getMapAsync(this);
        ed_searchAddress = findViewById(R.id.editTextTextPersonName);
        MyApplication myApplication = (MyApplication) getApplicationContext();
        savedLocations = myApplication.getMyLocation();
        lv_address = findViewById(R.id.lv_address);
        ArrayAdapter<String> lv_address_adapter = new ArrayAdapter<String>(this , android.R.layout.simple_expandable_list_item_1,lv_address_date);
        lv_address.setAdapter(lv_address_adapter);
        init();
    }

    private void init(){
        Log.d(TAG, "init: initializing");
        ed_searchAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        ||actionId == EditorInfo.IME_ACTION_DONE
                        ||event.getAction() == KeyEvent.ACTION_DOWN
                        ||event.getAction() == KeyEvent.KEYCODE_ENTER)
                {
                    geoLocate();
                }

                return false;
            }
        });


    }

    private void geoLocate() {
        Log.d(TAG, "geoLocate: geoLocateIog");
        String searchString = ed_searchAddress.getText().toString();
        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString,1);
        }catch (IOException e){
            Log.e(TAG, "geoLocate: IOException" + e.getMessage());
        }

        if (list.size()>0){
            Address address = list.get(0);
            //ed_searchAddress.setText("經度 : " + address.getLatitude() + "緯度 : "+ address.getLongitude() + "地址 : " + address.getAddressLine(0));
            mapMarkerAndZoomin(address.getLatitude(),address.getLongitude());
            Log.d(TAG, "geoLocate: found a location: "+address.toString());
        }

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
            Log.d(TAG, "onMapReady: ");
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLocationPlaced,17.0f));
        setMyLocation();

    }

    //  顯示定位圖層
    private void setMyLocation() throws SecurityException {
        mMap.setMyLocationEnabled(true); // 顯示定位圖層
    }

    private void mapMarkerAndZoomin(double latitude , double longitude) {
        LatLng latLng = new LatLng(latitude,longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Lat:"+latitude+"\t"+"Lon:"+longitude);
        mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,17.0f));
        Log.d(TAG, "mapMarker: ");
    }



}