package com.example.testble;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class ControlActivity extends ActionBarActivity {

    private RFduinoService rfduinoService;
    private String mDeviceName;
    private String mDeviceAddress;
    String TAG="er";
    Button btnBeep, btnDisconnected;
    TextView txtDevName, txtDevAddr, txtConnectState;
    private boolean mConnected = false;
    private android.os.Handler mHandler;
    SoundPool sp;
    int mid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra("DEVNAME");
        mDeviceAddress = intent.getStringExtra("DEVADDR");
        setTitle(mDeviceName);
        txtDevName=(TextView)findViewById(R.id.txtDeviceName);
        txtDevAddr=(TextView)findViewById(R.id.txtDeviceAddr);
        txtConnectState=(TextView)findViewById(R.id.txtConnectState);
        btnBeep=(Button)findViewById(R.id.btnBeep);
        btnDisconnected=(Button)findViewById((R.id.btnDis));

        txtDevName.setText(mDeviceName);
        txtDevAddr.setText(mDeviceAddress);

        btnDisconnected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rfduinoService != null) rfduinoService.disconnect();
                finish();
            }
        });

        btnBeep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rfduinoService != null && mConnected) {
                    rfduinoService.send(new byte[]{1});
                }
            }
        });

        mHandler = new android.os.Handler();
        sp = new SoundPool( 1, AudioManager.STREAM_MUSIC, 0);
        mid = sp.load(this, R.raw.beep, 1);

        Intent gattServiceIntent = new Intent(this, RFduinoService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void updateConnectionState() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtConnectState.setText(mConnected ? "Connected" : "Disconnected");
            }
        });
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState();
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState();
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] data=intent.getByteArrayExtra(RFduinoService.EXTRA_DATA);
                if(data!=null && data.length>=1 && data[0]==1) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            sp.play(mid, 1f,1f, 0, 0, 1f);
                        }
                    });
                }
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (!rfduinoService.initialize()) {
                Log.e("er", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            rfduinoService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            rfduinoService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (rfduinoService != null) {
            final boolean result = rfduinoService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        rfduinoService = null;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RFduinoService.ACTION_CONNECTED);
        intentFilter.addAction(RFduinoService.ACTION_DISCONNECTED);
        intentFilter.addAction(RFduinoService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
