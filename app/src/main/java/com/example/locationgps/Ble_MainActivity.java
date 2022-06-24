package com.example.locationgps;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.example.locationgps.adapter.DeviceAdapter;
import com.example.locationgps.comm.ObserverManager;
import com.example.locationgps.operation.OperationActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class Ble_MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = Ble_MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;

    private LinearLayout layout_setting;
    private TextView txt_setting;
    private Button btn_scan;
    private EditText et_name, et_mac, et_uuid;
    private Switch sw_auto;
    private ImageView img_loading;

    private Animation operatingAnim;
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog progressDialog;


    private String CHANNEL_ID = "Coder";
    MediaPlayer my;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_activity_main);
        initView();

        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);

        my = MediaPlayer.create(this,R.raw.sample);//音樂路徑

        /**檢查手機版本是否支援通知；若支援則新增"頻道"*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "DemoCode", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showConnectedDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                if (btn_scan.getText().equals(getString(R.string.start_scan))) {
                    checkPermissions();
                } else if (btn_scan.getText().equals(getString(R.string.stop_scan))) {
                    BleManager.getInstance().cancelScan();
                }
                break;

            case R.id.txt_setting:
                if (layout_setting.getVisibility() == View.VISIBLE) {
                    layout_setting.setVisibility(View.GONE);
                    txt_setting.setText(getString(R.string.expand_search_settings));
                } else {
                    layout_setting.setVisibility(View.VISIBLE);
                    txt_setting.setText(getString(R.string.retrieve_search_settings));
                }
                break;
        }
    }

    private void initView() {

        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_scan.setText(getString(R.string.start_scan));
        btn_scan.setOnClickListener(this);

        et_name = (EditText) findViewById(R.id.et_name);
        et_mac = (EditText) findViewById(R.id.et_mac);
        et_uuid = (EditText) findViewById(R.id.et_uuid);
        sw_auto = (Switch) findViewById(R.id.sw_auto);

        layout_setting = (LinearLayout) findViewById(R.id.layout_setting);
        txt_setting = (TextView) findViewById(R.id.txt_setting);
        txt_setting.setOnClickListener(this);
        layout_setting.setVisibility(View.GONE);
        txt_setting.setText(getString(R.string.expand_search_settings));

        img_loading = (ImageView) findViewById(R.id.img_loading);
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        operatingAnim.setInterpolator(new LinearInterpolator());
        progressDialog = new ProgressDialog(this);

        mDeviceAdapter = new DeviceAdapter(this);
        mDeviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onConnect(BleDevice bleDevice) {
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().cancelScan();
                    connect(bleDevice);
                }
            }

            @Override
            public void onDisConnect(final BleDevice bleDevice) {
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().disconnect(bleDevice);

                    /**建立要嵌入在通知裡的介面*/
                    RemoteViews view = new RemoteViews(getPackageName(),R.layout.custom_notification);

                    /**初始化Intent，攔截點擊事件*/
                    Intent intent = new Intent(Ble_MainActivity.this,NotificationReceiver.class);

                    /**設置通知內"Hi"這個按鈕的點擊事件(以Intent的Action傳送標籤，標籤為Hi)*/
                    intent.setAction("Hi");
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(Ble_MainActivity.this
                            ,0,intent,PendingIntent.FLAG_CANCEL_CURRENT);

                    /**設置通知內"Close"這個按鈕的點擊事件(以Intent的Action傳送標籤，標籤為Close)*/
                    intent.setAction("Close");
                    PendingIntent close = PendingIntent.getBroadcast(Ble_MainActivity.this
                            ,0,intent,PendingIntent.FLAG_CANCEL_CURRENT);

                    /**設置通知內的控件要做的事*/
                    /*設置標題*/
                    view.setTextViewText(R.id.textView_Title,"記得拔鑰匙！！！");
                    /*設置圖片*/
                    view.setImageViewResource(
                            R.id.imageView_Icon,R.drawable.ic_baseline_directions_bike_24);

                    /*設置"Hi"按鈕點擊事件(綁pendingIntent)*/
                    view.setOnClickPendingIntent(R.id.button_Noti_Hi,pendingIntent);
                    /*設置"Close"按鈕點擊事件(綁close)*/
                    view.setOnClickPendingIntent(R.id.button_Noti_Close,close);

                    /**建置通知欄位的內容*/
                    NotificationCompat.Builder builder
                            = new NotificationCompat.Builder(Ble_MainActivity.this,CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_baseline_directions_bike_24)
                            .setContent(view)
                            .setAutoCancel(true)
                            .setOnlyAlertOnce(true)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_MESSAGE);
                    NotificationManagerCompat notificationManagerCompat
                            = NotificationManagerCompat.from(Ble_MainActivity.this);
                    /**發出通知*/
                    notificationManagerCompat.notify(1,builder.build());
                    //發出聲音
                    Intent startIntent = new Intent(Ble_MainActivity.this, RingtonePlayingService.class);
                    Ble_MainActivity.this.startService(startIntent);
                    //發出震動
                    playVibrate();


                    Toast.makeText(Ble_MainActivity.this,"已斷開連線",Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onDetail(BleDevice bleDevice) {
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    Intent intent = new Intent(Ble_MainActivity.this, OperationActivity.class);
                    intent.putExtra(OperationActivity.KEY_DATA, bleDevice);
                    startActivity(intent);
                }
            }
        });
        ListView listView_device = (ListView) findViewById(R.id.list_device);
        listView_device.setAdapter(mDeviceAdapter);
    }

    public void playVibrate()
    {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = { 0, 1000, 500, 1000, 500, 1000, 500, 1000, 500};
        v.vibrate(pattern , -1);
    }



    private void showConnectedDevice() {
        List<BleDevice> deviceList = BleManager.getInstance().getAllConnectedDevice();
        mDeviceAdapter.clearConnectedDevice();
        for (BleDevice bleDevice : deviceList) {
            mDeviceAdapter.addDevice(bleDevice);
        }
        mDeviceAdapter.notifyDataSetChanged();
    }

    private void setScanRule() {
        String[] uuids;
        String str_uuid = et_uuid.getText().toString();
        if (TextUtils.isEmpty(str_uuid)) {
            uuids = null;
        } else {
            uuids = str_uuid.split(",");
        }
        UUID[] serviceUuids = null;
        if (uuids != null && uuids.length > 0) {
            serviceUuids = new UUID[uuids.length];
            for (int i = 0; i < uuids.length; i++) {
                String name = uuids[i];
                String[] components = name.split("-");
                if (components.length != 5) {
                    serviceUuids[i] = null;
                } else {
                    serviceUuids[i] = UUID.fromString(uuids[i]);
                }
            }
        }

        String[] names;
        String str_name = et_name.getText().toString();
        if (TextUtils.isEmpty(str_name)) {
            names = null;
        } else {
            names = str_name.split(",");
        }

        String mac = et_mac.getText().toString();

        boolean isAutoConnect = sw_auto.isChecked();

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
                .setDeviceName(true, names)   // 只扫描指定广播名的设备，可选
                .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
                .setAutoConnect(isAutoConnect)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                mDeviceAdapter.clearScanDevice();
                mDeviceAdapter.notifyDataSetChanged();
                img_loading.startAnimation(operatingAnim);
                img_loading.setVisibility(View.VISIBLE);
                btn_scan.setText(getString(R.string.stop_scan));
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));
            }
        });
    }

    private void connect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                progressDialog.show();
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));
                progressDialog.dismiss();
                Toast.makeText(Ble_MainActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                progressDialog.dismiss();
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                progressDialog.dismiss();

                mDeviceAdapter.removeDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();

                /**建立要嵌入在通知裡的介面*/
                RemoteViews view = new RemoteViews(getPackageName(),R.layout.custom_notification);

                /**初始化Intent，攔截點擊事件*/
                Intent intent = new Intent(Ble_MainActivity.this,NotificationReceiver.class);

                /**設置通知內"Hi"這個按鈕的點擊事件(以Intent的Action傳送標籤，標籤為Hi)*/
                intent.setAction("Hi");
                PendingIntent pendingIntent = PendingIntent.getBroadcast(Ble_MainActivity.this
                        ,0,intent,PendingIntent.FLAG_CANCEL_CURRENT);

                /**設置通知內"Close"這個按鈕的點擊事件(以Intent的Action傳送標籤，標籤為Close)*/
                intent.setAction("Close");
                PendingIntent close = PendingIntent.getBroadcast(Ble_MainActivity.this
                        ,0,intent,PendingIntent.FLAG_CANCEL_CURRENT);

                /**設置通知內的控件要做的事*/
                /*設置標題*/
                view.setTextViewText(R.id.textView_Title,"記得拔鑰匙！！！");
                /*設置圖片*/
                view.setImageViewResource(
                        R.id.imageView_Icon,R.drawable.ic_baseline_directions_bike_24);

                /*設置"Hi"按鈕點擊事件(綁pendingIntent)*/
                view.setOnClickPendingIntent(R.id.button_Noti_Hi,pendingIntent);
                /*設置"Close"按鈕點擊事件(綁close)*/
                view.setOnClickPendingIntent(R.id.button_Noti_Close,close);

                /**建置通知欄位的內容*/
                NotificationCompat.Builder builder
                        = new NotificationCompat.Builder(Ble_MainActivity.this,CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_baseline_directions_bike_24)
                        .setContent(view)
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE);
                NotificationManagerCompat notificationManagerCompat
                        = NotificationManagerCompat.from(Ble_MainActivity.this);
                /**發出通知*/
                notificationManagerCompat.notify(1,builder.build());
                //發出聲音
                Intent startIntent = new Intent(Ble_MainActivity.this, RingtonePlayingService.class);
                Ble_MainActivity.this.startService(startIntent);
                //發出震動
                playVibrate();
                if (isActiveDisConnected) {
                    Toast.makeText(Ble_MainActivity.this, getString(R.string.active_disconnected), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(Ble_MainActivity.this, getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                    ObserverManager.getInstance().notifyObserver(bleDevice);
                }

            }
        });
    }

    private void readRssi(BleDevice bleDevice) {
        BleManager.getInstance().readRssi(bleDevice, new BleRssiCallback() {
            @Override
            public void onRssiFailure(BleException exception) {
                Log.i(TAG, "onRssiFailure" + exception.toString());
            }

            @Override
            public void onRssiSuccess(int rssi) {
                Log.i(TAG, "onRssiSuccess: " + rssi);
            }
        });
    }

    private void setMtu(BleDevice bleDevice, int mtu) {
        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                Log.i(TAG, "onsetMTUFailure" + exception.toString());
            }

            @Override
            public void onMtuChanged(int mtu) {
                Log.i(TAG, "onMtuChanged: " + mtu);
            }
        });
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }

    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    setScanRule();
                    startScan();
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                setScanRule();
                startScan();
            }
        }
    }

}
