package com.javacodegeeks.androidaccelerometerexample;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.telephony.SmsManager;

import com.javacodegeeks.androidaccelerometerexample.ble.DeviceListActivity;
import com.javacodegeeks.androidaccelerometerexample.ble.UartService;
import com.javacodegeeks.androidaccelerometerexample.push.PushNotifications;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class AndroidAccelerometerExample extends Activity implements SensorEventListener, TextToSpeech.OnInitListener, RadioGroup.OnCheckedChangeListener {

    public static final String TAG = AndroidAccelerometerExample.class.getName();

    private SensorManager sensorManager;
	private Sensor accelerometer;

    private Queue<float[]> sensorEventQueue;

    private float vibrateThreshold = 0;
    private Handler mHandler;

    private int count;

	private TextView countView, notifyTimeRangeView;

	public Vibrator v;
    TextToSpeech ttobj;

    private MyMeteor mMeteor;

    public AccelQueue accelQueue;


    private LocationMonitor locationMonitor;

    private boolean isProduction = false;
    public boolean alarmTriggered = false;
    private PBullet pbullet;

    private PowerManager.WakeLock mWakeLock = null;


    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    TextView mRemoteRssiVal;
    RadioGroup mRg;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
//    private ListView messageListView;
//    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect,btnSend;
    private EditText edtMessage;

    /*
 * Register this as a sensor event listener.
 */
    private void registerListener() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /*
     * Un-register this as a sensor event listener.
     */
    private void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
        sensorEventQueue = new ConcurrentLinkedQueue<float[]>();
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		initializeViews();

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			// success! we have an accelerometer

			registerListener();
			vibrateThreshold = (float) .5;
		} else {
			// fail! we dont have an accelerometer!
		}

		//initialize vibration
		v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        count = 0;

        setupTimeUpdate();

        accelQueue = new AccelQueue();

        ttobj = new TextToSpeech(getApplicationContext(), this);

        mMeteor = new MyMeteor(this);

        Log.d("mine", "Push creating, at the start");
        PushNotifications pusher = new PushNotifications(getApplicationContext(), this);


        locationMonitor = new LocationMonitor(this);

        ttobj = new TextToSpeech(getApplicationContext(), this);
        pbullet = new PBullet();

        PowerManager manager =
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        // Needed to have accelerometer readings stay up to date, and not delay when the screen is off.
        mWakeLock.acquire();
        bluetoothSetup();
    }



    @Override
    public void onInit(int status) {
        if(status != TextToSpeech.ERROR){
            ttobj.setLanguage(Locale.UK);
        }
    }

    private void sendTTSReceived() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                //code to do the HTTP request
                HttpClient httpClient = new DefaultHttpClient();
                try {
                    Log.d("mine", "Sending tts received");
                    HttpPost request = new HttpPost("http://biker.chaselambda.com/tts-received");
                    request.addHeader("content-type", "application/json");
                    httpClient.execute(request);
                } catch (Exception ex) {
                    // handle exception here
                    Log.d("mine", "FAILED request");
                } finally {
                    httpClient.getConnectionManager().shutdown();
                }
            }
        });
        thread.start();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
//        setIntent(intent);//must store the new intent unless getIntent() will return the old one
        String intentText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (intentText == null) {
            Log.e("mine", "Intent text equals null for some reason");
            return;
        }
        Log.d("mine", "Got intent text: " + intentText);
        if (intentText.equals("gps-off")) {
            locationMonitor.gpsOff();
            Log.d("mine", "GPS OFF");
        } else if (intentText.equals("gps-on")) {
            locationMonitor.gpsOn();
            Log.d("mine", "GPS ON");
        } else if (intentText.equals("prod")) {
            isProduction = true;
            Log.d("mine", "PRODUCTION ON");
        } else if (intentText.equals("debug")) {
            isProduction = false;
            Log.d("mine", "PRODUCTION OFF");
        } else if (intentText.equals("alarm-reset")) {
            alarmTriggered = false;
            sensorEventQueue.clear();
        updateNotifyTimeRangeView();
            locationMonitor.gpsOff();
        } else if (intentText.equals("alarm-trigger")) {
            alarmTriggered = true;
            mMeteor.alarmTrigger();
            ttobj.speak("Artifically triggered.", TextToSpeech.QUEUE_FLUSH, null);
            locationMonitor.gpsOn();
        } else {
            ttobj.speak(intentText, TextToSpeech.QUEUE_FLUSH, null);
        }
        sendTTSReceived();
    }

    private void updateNotifyTimeRangeView() {
        if (alarmTriggered) {
            notifyTimeRangeView.setText("Alarm triggered!");
        } else {
            notifyTimeRangeView.setText("Notification are sleeping");
        }
    }

    private void setupTimeUpdate() {
        mHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                updateNotifyTimeRangeView();
                            }
                        });
                    } catch (Exception e) {
                        Log.e("mine", "TODO something bad here, not sure what to do");
                    }
                }
            }
        }).start();
    }

    private float maxAccelDifference(float[] current) {
        float ret = 0;
        for (float[] arr : sensorEventQueue) {
            ret = Math.max(ret, (float) Math.sqrt(
                        Math.pow(arr[0] - current[0], 2) +
                        Math.pow(arr[1] - current[1], 2) +
                        Math.pow(arr[2] - current[2], 2)));
        }
        return ret;
    }

	public void initializeViews() {
        countView = (TextView) findViewById(R.id.count);
        notifyTimeRangeView = (TextView) findViewById(R.id.notifyTimeRange);


    }

	////onResume() register the accelerometer for listening the events
	//protected void onResume() {
		//super.onResume();
		//sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	//}

	////onPause() unregister the accelerometer for stop listening the events
	//protected void onPause() {
		//super.onPause();
		//sensorManager.unregisterListener(this);
	//}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
        float[] current = {event.values[0], event.values[1], event.values[2]};
        AccelTime accelTime = new AccelTime(event.values[0], event.values[1], event.values[2], System.currentTimeMillis());
        accelQueue.accelsToSend.add(accelTime);


        count += 1;
//        if (count % 10 == 0) {
//            Log.d("mine", accelsToJSON());
//            Log.d("mine", "Attempt to send at count " + count);
////            Arrays.fill(accelsToSend, (float) 0);
//        }
        countView.setText(Integer.toString(count));
		maybeVibrate(current); // TODO bring back
	}

        // if the change in the accelerometer value is big enough, then vibrate!
	// our threshold is MaxValue/2
	public void maybeVibrate(float[] current) {
        if (maxAccelDifference(current) > vibrateThreshold) {
            if (!alarmTriggered) {
                alarmTriggered = true;
                Log.d("mine", "Actual notify. Sending sms!");
                Date date = new Date();
                if (isProduction) {
                    ttobj.speak("Welcome to the lock free bike. If you would like this moved, please call the number located on the handlebars.", TextToSpeech.QUEUE_FLUSH, null);
//                    ttobj.speak("rough", TextToSpeech.QUEUE_FLUSH, null);

                    pbullet.send("Phone moved!", "At " + date.toString());
                    SmsManager.getDefault().sendTextMessage("5125778778", null, "Phone moved -- " + date.toString(), null,null);
                    Toast.makeText(getApplicationContext(), "Sending SMS!", Toast.LENGTH_SHORT).show();
                } else {
                    v.vibrate(50);
                }
                mMeteor.alarmTrigger();
                locationMonitor.gpsOn();
            }
            updateNotifyTimeRangeView();


            Log.d("mine", "Diff is great enough!");
            sensorEventQueue.clear();
        }
        sensorEventQueue.add(current);
        if (sensorEventQueue.size() > 100) {
            sensorEventQueue.poll();
        }

	}
//
//    private void startSendingServerData() {
//        Thread thread = new Thread(new Runnable(){
//            @Override
//            public void run(){
//                //code to do the HTTP request
//                while (true) {
//                    HttpClient httpClient = new DefaultHttpClient();
//                    try {
//
//                        HttpPost request = new HttpPost("http://biker.chaselambda.com/multi_accels/");
//                        Log.d("mine", "get JSON");
//                        StringEntity params = new StringEntity(accelQueue.accelsToJSON());
//                        Log.d("mine", "got JSON");
//                        request.addHeader("content-type", "application/json");
//                        request.setEntity(params);
//                        Log.d("mine", "Attempt to send for count" + count);
//                        HttpResponse response = httpClient.execute(request);
////                response.getStatusLine().getStatusCode();
//                        Log.d("mine", "SUCCESS request for count " + count);
//
//                    } catch (Exception ex) {
//                        // handle exception here
//                        Log.d("mine", "FAILED request");
//                    } finally {
//                        httpClient.getConnectionManager().shutdown();
//                    }
//                }
//            }
//        });
//        thread.start();
//    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        Log.d("mine", "onstart called");
//        locationMonitor.mGoogleApiClient.connect();
//        Log.d("mine", "THEN CALLED connect()");
//    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        Log.d(TAG, "STOP!");
//        mWakeLock.release();
//        mMeteor.mMeteor.disconnect();
//        unregisterListener();
////        locationMonitor.mGoogleApiClient.purposefulDisconnect(); // DON't stop, we want to run this in the background
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DESTROY!");
        mWakeLock.release();
        mMeteor.mMeteor.disconnect();
        unregisterListener();

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;
    }






























    private void bluetoothSetup() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
//        messageListView = (ListView) findViewById(R.id.listMessage);
//        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
//        messageListView.setAdapter(listAdapter);
//        messageListView.setDivider(null);
        btnConnectDisconnect=(Button) findViewById(R.id.btn_select);
        btnSend=(Button) findViewById(R.id.sendButton);
        edtMessage = (EditText) findViewById(R.id.sendText);
        service_init();



        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                    if (btnConnectDisconnect.getText().equals("Connect")){

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(AndroidAccelerometerExample.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice!=null)
                        {
                            mService.disconnect();

                        }
                    }
                }
            }
        });
        // Handler Send button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.sendText);
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


    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
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

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        edtMessage.setEnabled(true);
                        btnSend.setEnabled(true);
                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
//                        listAdapter.add("["+currentDateTimeString+"] Connected to: "+ mDevice.getName());
//                        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        edtMessage.setEnabled(false);
                        btnSend.setEnabled(false);
                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
//                        listAdapter.add("["+currentDateTimeString+"] Disconnected to: "+ mDevice.getName());
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
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
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String text = new String(txValue, "UTF-8");
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
//                            listAdapter.add("["+currentDateTimeString+"] RX: "+text);
//                            messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);

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
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
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

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }
}
