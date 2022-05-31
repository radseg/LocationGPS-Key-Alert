package com.example.locationgps;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class gps_main extends AppCompatActivity {
    public ListView lv_address;
    String[] lv_address_date = new String[] {"我家","學校","工作場所","籃球場","新天地","7-11","7-11","7-11","7-11","7-11","7-11"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gps_main);
        lv_address = findViewById(R.id.lv_address);
        ArrayAdapter<String> lv_address_adapter = new ArrayAdapter<String>(this , android.R.layout.simple_expandable_list_item_1,lv_address_date);
        lv_address.setAdapter(lv_address_adapter);


    }
}
