package com.example.locationgps;

import android.content.DialogInterface;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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

import androidx.appcompat.app.AlertDialog;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.FragmentActivity;

import com.example.locationgps.SQLiteDB.Address_DB;
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
    public Button btn_listview_add;
    public Spinner sp_custom;
    private GoogleMap mMap;
    //private ActivityMapsBinding binding;
    private Address_DB address_db=null;
    public long listview_id;  // 儲存 _id 的值
    public double lastSearch_lat=0,lastSearch_lng=0;
    public String lastSearch_custom=null,lastSearch_address=null,lastSearch_place=null;
    String[] lv_address_date = new String[] {"我家","學校","工作場所","籃球場","新天地","7-11","7-11","7-11","7-11","7-11","7-11"};
    String[] custom =new String[]{"其他","我家","工作地"} ;
    List<Location> savedLocations;
    Cursor cursor;
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
        btn_listview_add = findViewById(R.id.btn_listview_add);
        sp_custom = findViewById(R.id.sp_custom);
        lv_address.setOnItemClickListener(addressListViewListener);
        btn_listview_add.setOnClickListener(btn_addressListViewListener);

        //spinner
        sp_custom.setOnItemSelectedListener(sp_customListener);
        ArrayAdapter<String> custom_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,custom);
        sp_custom.setAdapter(custom_adapter);

        //map載入
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

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




        keyboardSearch();

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
                }
                return false;
            }
        });
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
        setMyLocation();

    }

    //  顯示定位圖層
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


    private final Button.OnClickListener btn_addressListViewListener=new Button.OnClickListener(){
        public void onClick(View v){
            try{
                if (v.getId() == R.id.btn_listview_add){// 新增
                    String st_custom=sp_custom.getSelectedItem().toString();
                    String st_place=ed_searchAddress.getText().toString();
                    String st_ad=ed_searchAddress.getText().toString();
                    Double st_lat=Double.parseDouble(ed_searchAddress.getText().toString());
                    Double st_lng=Double.parseDouble(ed_searchAddress.getText().toString());

                    if ( address_db.append(st_custom,st_place,st_ad,st_lat,st_lng)>0){
                        cursor=address_db.getAll();// 載入全部資料
                        UpdateAdapter(cursor);  // 載入資料表至 ListView 中
                    }
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

    }




    @Override
    protected void onDestroy(){
        super.onDestroy();
        address_db.close(); // 關閉資料庫
    }

}