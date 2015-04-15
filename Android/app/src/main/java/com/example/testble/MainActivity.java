package com.example.testble;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;

    private boolean bScan=false;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 5000;
    private BLEDeviceListAdapter devs;
    private ListView listView;
    private BluetoothLeAdvertiser ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Blueinno Buzzer");

        mHandler = new Handler();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        listView = (ListView)findViewById(R.id.listView);
        listView.setDividerHeight(5);
        devs = new BLEDeviceListAdapter();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if(bScan)
        {
            menu.findItem(R.id.action_stop).setEnabled(true);
            menu.findItem(R.id.action_scan).setEnabled(false);
            menu.findItem(R.id.action_searching).setActionView(R.layout.bar_progitem);
        }
        else {
            menu.findItem(R.id.action_stop).setEnabled(false);
            menu.findItem(R.id.action_scan).setEnabled(true);
            menu.findItem(R.id.action_searching).setActionView(null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_scan)
        {
            StartScan();
            return true;
        }
        else if(id ==R.id.action_stop) {
            StopScan();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        devs = new BLEDeviceListAdapter();
        listView.setAdapter(devs);
        StartScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        StopScan();
        devs.clear();
        devs.notifyDataSetChanged();
    }

    private void StartScan() {
        devs.clear();
        devs.notifyDataSetChanged();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bScan = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                invalidateOptionsMenu();
            }
        }, SCAN_PERIOD);

        bScan = true;
        mBluetoothAdapter.startLeScan(
                new UUID[]{RFduinoService.UUID_SERVICE},
                mLeScanCallback);
        invalidateOptionsMenu();
    }

    private void StopScan() {
        bScan = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            devs.addDevice(device);
                            devs.notifyDataSetChanged();
                        }
                    });
                }
            };

    private class BLEDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> devs;
        private LayoutInflater mInflator;

        public BLEDeviceListAdapter() {
            super();
            devs = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!devs.contains(device)) {
                devs.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return devs.get(position);
        }

        public void clear() {
            devs.clear();
        }

        @Override
        public int getCount() {
            return devs.size();
        }

        @Override
        public Object getItem(int i) {
            return devs.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            final ViewHolder viewHolder;
            final BluetoothDevice device = devs.get(i);

            if (view == null) {
                view = mInflator.inflate(R.layout.list_devitem, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.devaddr);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.devname);
                viewHolder.btnBeep = (Button)view.findViewById(R.id.btnbeep);
                viewHolder.device = device;
                viewHolder.btnBeep.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        /*
                        BluetoothGatt gatt = device.connectGatt(MainActivity.this, false, mGattCallback);
                        if(gatt!=null) {

                            BluetoothGattCharacteristic c = new BluetoothGattCharacteristic(RFduinoService.UUID_SERVICE, BluetoothGattCharacteristic.PERMISSION_WRITE, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE);
                            c.setValue(new byte[]{1});
                            c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                            gatt.writeCharacteristic(c);
                            gatt.executeReliableWrite();

                            gatt.disconnect();
                            gatt.close();

                        }
                        */
                        if(device!=null) {
                            final Intent intent = new Intent(MainActivity.this, ControlActivity.class);
                            intent.putExtra("DEVNAME", device.getName());
                            intent.putExtra("DEVADDR", device.getAddress());
                            startActivity(intent);
                        }
                    }
                });
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unknown");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,  int newState) {
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        Button btnBeep;
        BluetoothDevice device;
    }
}
