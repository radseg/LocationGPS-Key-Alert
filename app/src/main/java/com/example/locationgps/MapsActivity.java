package com.example.locationgps;

import android.Manifest;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.FragmentActivity;

import com.example.locationgps.Geofence.GeofenceHelper;
import com.example.locationgps.SQLiteDB.Address_DB;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    public static final String TAG = "MapsActivity";
    public static final int PERMISSIONS_FINE_LOCATION = 99;
    FusedLocationProviderClient fusedLocationProviderClient;
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    public long listview_id;  // 儲存 _id 的值
    public double lastSearch_lat=0,lastSearch_lng=0;
    public String lastSearch_custom=null,lastSearch_address=null,lastSearch_place=null;
    public ListView lv_address;
    public EditText ed_searchAddress;
    public Button btn_listview_clear,btn_listview_go;
    public Spinner sp_custom;
    private GoogleMap mMap;
    LocationRequest locationRequest;
    //private ActivityMapsBinding binding;
    private Address_DB address_db=null;
    String[] custom =new String[]{"其他","我家","工作地"} ;
    List<Location> savedLocations;
    Cursor cursor;

    private static final float GEOFENCE_RADIUS = 200;
    private static final String GEOFENCE_ID = "SOME_GEOFENCE_ID";

    private static final int FINE_LOCATION_ACCESS_REQUEST_CODE = 10001;
    private static final int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002;


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

        lv_address = findViewById(R.id.lv_address);
        ed_searchAddress = findViewById(R.id.editTextTextPersonName);
        btn_listview_clear = findViewById(R.id.btn_listview_clear);
        btn_listview_go = findViewById(R.id.btn_listview_go);
        sp_custom = findViewById(R.id.sp_custom);
        lv_address.setOnItemClickListener(addressListViewListener);
        btn_listview_clear.setOnClickListener(btn_addressListViewListener);
        btn_listview_go.setOnClickListener(btn_addressListViewListener);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //map載入
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);

        //鍵盤觸發事件
        keyboardSearch();



        //spinner
        sp_custom.setOnItemSelectedListener(sp_customListener);
        ArrayAdapter<String> custom_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,custom);
        sp_custom.setAdapter(custom_adapter);

        lastSearch_custom=sp_custom.getSelectedItem().toString();
        lastSearch_place="中興大學";
        lastSearch_address="402台灣台中市南區興大路145號";
        lastSearch_lat=24.123552;
        lastSearch_lng=120.675326;


        //DB載入
        address_db =new Address_DB(this);// 建立 MyDB 物件
        address_db.open();
        cursor=address_db.getAll();
        if (cursor != null && cursor.getCount() > 0){
            cursor=address_db.getAll();// 載入全部資料
            UpdateAdapter(cursor);  // 載入資料表至 ListView 中
            Log.d(TAG, "onCreate: dbData=true");
        }else{
            address_db.append(lastSearch_custom,lastSearch_place,lastSearch_address,lastSearch_lat,lastSearch_lng);
            cursor=address_db.getAll();// 載入全部資料
            UpdateAdapter(cursor);  // 載入資料表至 ListView 中
            Log.d(TAG, "onCreate: dbData=false");
        }


        //listView長按點擊事件
        lv_address.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (cursor != null && cursor.getCount() >= 0){
                    AlertDialog.Builder builder=new AlertDialog.Builder(MapsActivity.this);
                    builder.setTitle("確定刪除");
                    builder.setMessage("確定要刪除" + ed_searchAddress.getText() + "這筆資料?");
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                        }
                    });
                    builder.setPositiveButton("確定",new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            if (address_db.delete(listview_id)){
                                cursor=address_db.getAll();// 載入全部資料
                                UpdateAdapter(cursor); // 載入資料表至 ListView 中
                                ClearEdit();
                            }
                        }
                    });
                    builder.show();
                }
                return false;
            }
        });



        updateGPS();
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
        MyApplication myApplication = (MyApplication) getApplicationContext();
        savedLocations = myApplication.getMyLocation();
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


    }

    private void getBackgroundPermission_addGeofence(double latitude ,double longitude){

        LatLng latLng = new LatLng(latitude,longitude);
        if (Build.VERSION.SDK_INT >= 29) {
            //We need background permission
            if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mapShowGeofence(latLng);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    //We show a dialog and ask for permission
                    ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                } else {
                    ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                }
            }

        } else {
            mapShowGeofence(latLng);
        }
    }




    private void mapShowGeofence(LatLng latLng) {
        addMarker(latLng);
        addCircle(latLng, GEOFENCE_RADIUS);
        addGeofence(latLng, GEOFENCE_RADIUS);
    }

    private void addGeofence(LatLng latLng, float radius) {

        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: Geofence Added...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = geofenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure: " + errorMessage);
                    }
                });
    }

    private void addMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mMap.addMarker(markerOptions);
    }

    private void addCircle(LatLng latLng, float radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255, 255, 0,0));
        circleOptions.fillColor(Color.argb(64, 255, 0,0));
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);
    }

    // 使用者完成授權的選擇以後，會呼叫 onRequestPermissionsResult 方法
    //     第一個參數requestCode：請求授權代碼
    //     第二個參數permissions：請求的授權名稱
    //     第三個參數grantResults：使用者選擇授權的結果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                    setMyLocation();
                } else {
                    Toast.makeText(this, "This app requests permission to be granted in order to work properly", Toast.LENGTH_LONG).show();
                }
        }
    }

    //拿到GPS權限，並更新畫面顯示擁有權限
    private void updateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //從使用者按下給權限後執行的動作
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    setMyLocation();
                }
            });
        } else {
            //使用者沒給權限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //檢查手機版本是不是6.0以上
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }



    // 顯示定位圖層，一定要拿完權限才能用這個指令，要不然會閃退
    private void setMyLocation() throws SecurityException {
        mMap.setMyLocationEnabled(true); // 顯示定位圖層
    }

    private void mapMarkerAndZoomIn(double latitude , double longitude) {
        LatLng latLng = new LatLng(latitude,longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Lat:"+latitude+"\t"+"Lon:"+longitude);
        mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,17.0f));
        Log.d(TAG, "mapMarker: ");
    }



    //搜尋地點拿到地址、緯度、經度並標記該紅點畫面移動過去
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
            mapMarkerAndZoomIn(address.getLatitude(),address.getLongitude());

            //搜尋的值儲存起來
            lastSearch_custom = sp_custom.getSelectedItem().toString();
            lastSearch_place = ed_searchAddress.getText().toString();
            lastSearch_address = address.getAddressLine(0);
            lastSearch_lat = address.getLatitude();
            lastSearch_lng = address.getLongitude();

            //自動加入資料庫並在listView顯示出來
            if ( address_db.append(lastSearch_custom,lastSearch_place,lastSearch_address,lastSearch_lat,lastSearch_lng)>0){
                cursor=address_db.getAll();// 載入全部資料
                UpdateAdapter(cursor);  // 載入資料表至 ListView 中
            }

            Log.d(TAG, "geoLocate: found a location: "+address.toString());
        }
    }

    //SpinnerItem點擊事件
    private Spinner.OnItemSelectedListener sp_customListener=
            new Spinner.OnItemSelectedListener(){

                @Override
                public void onItemSelected(AdapterView<?> parent, View v,
                                           int position, long id) {
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            };

    //鍵盤點擊事件
    private void keyboardSearch(){
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
                    //搜尋時會把最後的位置變數存起來
                    getBackgroundPermission_addGeofence(lastSearch_lat,lastSearch_lng);
                }
                return false;
            }
        });
    }


    //Button點擊事件
    private final Button.OnClickListener btn_addressListViewListener=new Button.OnClickListener(){
        public void onClick(View v){
            try{
                if (v.getId() == R.id.btn_listview_clear){
                    mMap.clear();
                    Toast.makeText(MapsActivity.this,"清除紅點選擇",Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onClick: btn_listview_add");
                }else if (v.getId() == R.id.btn_listview_go){
                    Toast.makeText(MapsActivity.this,"出發",Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onClick: btn_listview_del");
                }

            }catch (Exception err){
                Toast.makeText(getApplicationContext(), "資料不正確!", Toast.LENGTH_SHORT).show();
            }
        }
    };





    public void UpdateAdapter(Cursor cursor){
        if (cursor != null && cursor.getCount() >= 0){
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_2, // 包含兩個資料項
                    cursor, // 資料庫的 Cursors 物件
                    new String[] {"place","address"}, // place、address 欄位
                    new int[] { android.R.id.text1, android.R.id.text2 },
                    0);
            lv_address.setAdapter(adapter); // 將adapter增加到listview中
        }
    }

    public void ClearEdit(){
        ed_searchAddress.setText("");
    }

    //listView點擊事件
    private final ListView.OnItemClickListener addressListViewListener=
            new ListView.OnItemClickListener(){
                public void onItemClick(AdapterView<?> parent, View v,
                                        int position, long id) {
                    ShowData(id);
                    cursor.moveToPosition(position);

                }
            };



    private void ShowData(long id){ //顯示單筆資料
        Cursor cursor=address_db.get(id);
        listview_id=id;  // 取得  _id 欄位
        mapMarkerAndZoomIn(cursor.getDouble(4),cursor.getDouble(5));
        getBackgroundPermission_addGeofence(cursor.getDouble(4),cursor.getDouble(5));
    }




    @Override
    protected void onDestroy(){
        super.onDestroy();
        address_db.close(); // 關閉資料庫
    }


}