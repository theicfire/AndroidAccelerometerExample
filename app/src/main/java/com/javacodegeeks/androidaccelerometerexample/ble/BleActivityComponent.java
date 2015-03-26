package com.javacodegeeks.androidaccelerometerexample.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.javacodegeeks.androidaccelerometerexample.BikeChain;
import com.javacodegeeks.androidaccelerometerexample.BikeLEDLights;
import com.javacodegeeks.androidaccelerometerexample.MainActivity;
import com.javacodegeeks.androidaccelerometerexample.R;
import com.javacodegeeks.androidaccelerometerexample.Utils;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;

/**
 * Created by chase on 3/19/15.
 *
 */
public class BleActivityComponent implements RadioGroup.OnCheckedChangeListener{
    public static final String TAG = "BleActivityComponent";

    private static final int REQUEST_SELECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    TextView mRemoteRssiVal;
    RadioGroup mRg;
    private int mState = UART_PROFILE_DISCONNECTED;
    public UartService mService = null;
    private BluetoothDevice mDevice = null;
    public BluetoothAdapter mBtAdapter = null;
    //    private ListView messageListView;
//    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect,btnSend;
    private EditText edtMessage;
    public final MainActivity activity;

    public BleActivityComponent(MainActivity act) {
        this.activity = act;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
            return;
        }
//        messageListView = (ListView) findViewById(R.id.listMessage);
//        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
//        messageListView.setAdapter(listAdapter);
//        messageListView.setDivider(null);
        btnConnectDisconnect=(Button) activity.findViewById(R.id.btn_select);
        btnSend=(Button) activity.findViewById(R.id.sendButton);
        edtMessage = (EditText) activity.findViewById(R.id.sendText);
        service_init();

        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                    if (btnConnectDisconnect.getText().equals("Connect")){
                        arduinoConnect();
                    } else {
                        disconnect();
                    }
                }
            }
        });
        // Handler Send button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) activity.findViewById(R.id.sendText);
                String message = editText.getText().toString();
                byte[] value;
                try {
                    //send data to service
                    value = message.getBytes("UTF-8");
                    mService.writeRXCharacteristic(value);
                    //Update the log with time stamp
                    String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
//                    listAdapter.add("["+currentDateTimeString+"] TX: "+ message);
//                    messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                    edtMessage.setText("");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });
    }

    public void arduinoConnect() {
        String deviceAddress = "D8:8C:7B:9F:AA:5B";
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

        ((TextView) activity.findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");

        mService.connect(deviceAddress);
    }

    public void disconnect() {
        if (mDevice!=null) {
            mService.disconnect();
        }
    }


    //UART service connected/disconnected
    public ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();

            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                activity.finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };
//
//    private Handler mHandler = new Handler() {
//        @Override
//
//        //Handler events that received from UART service
//        public void handleMessage(Message msg) {
//
//        }
//    };
    public void reconnect() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO I'd love to have some callback on the attempted reconnect to know if the reconnect failed or not.
                // But UartService doesn't seem to tell us when the reconnect attempt failed...
                if (mState == UART_PROFILE_DISCONNECTED) {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    Log.d(TAG, "Reconnecting bluetooth");
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            arduinoConnect();
                        }
                    });
                }
            }
        });
        thread.start();

    }

    public final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                // TODO this callback is not accurate. If I tell the arduino to say something whenever it connects,
                // there can be a significant delay between when this is hit and when the message from the arduino
                // is received.
                Log.d(TAG, "Almost connected");
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        edtMessage.setEnabled(false);
                        btnSend.setEnabled(false);
                        ((TextView) activity.findViewById(R.id.deviceName)).setText("Not Connected");
                        Utils.postReqTask("http://biker.chaselambda.com/setGlobalState/bluetoothOn/false");
//                        listAdapter.add("["+currentDateTimeString+"] Disconnected to: "+ mDevice.getName());
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();

//                        reconnect();
//                        arduinoConnect();

                        //setUiState();

                    }
                });
            }


            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String text = new String(txValue, "UTF-8");
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
//                            listAdapter.add("["+currentDateTimeString+"] RX: "+text);
//                            messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);

                            Log.d(TAG, "Received text " + text);

                            if ("connected".equals(text)) {
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                                        Log.d(TAG, "UART_CONNECT_MSG");
                                        btnConnectDisconnect.setText("Disconnect");
                                        edtMessage.setEnabled(true);
                                        btnSend.setEnabled(true);
                                        ((TextView) activity.findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                                        Utils.postReqTask("http://biker.chaselambda.com/setGlobalState/bluetoothOn/true");
//                        listAdapter.add("["+currentDateTimeString+"] Connected to: "+ mDevice.getName());
//                        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                                        mState = UART_PROFILE_CONNECTED;
                                    }
                                });
                            } else if ("lon".equals(text)) {
                                BikeLEDLights.turnOnCallback();
                            } else if ("loff".equals(text)) {
                                BikeLEDLights.turnOffCallback();
                            } else if ("chainon".equals(text)) {
                                BikeChain.turnOnCallback();
                            } else if ("chainoff".equals(text)) {
                                activity.sendChainAlert();
                                BikeChain.turnOffCallback();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };

    private void service_init() {
        Intent bindIntent = new Intent(activity, UartService.class);
        activity.bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(activity).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }



    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) activity.findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    Log.d(TAG, "deviceAddress " + deviceAddress);
                    mService.connect(deviceAddress);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(activity, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(activity, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    activity.finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }
}
