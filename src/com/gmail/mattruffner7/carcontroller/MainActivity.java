package com.gmail.mattruffner7.carcontroller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	// Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    private static final int LEFT = 3;
    private static final int RIGHT = 4;
    private static final int STRAIGHT = 5;
	
    public static final boolean DEBUG = true;
    
    // the log tag so its clear on the debug output
 	public static final String LOG_TAG = "Car Controller";
    
 	// Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
 	
 	// constants received from the BluetoothSerialService
 	public static final int MESSAGE_STATE_CHANGE = 1;
 	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
 	
	BluetoothAdapter mBluetoothAdapter;
	
	private static BluetoothSerialService mSerialService = null;
    
    private MenuItem mMenuItemConnect;
    
    private String mConnectedDeviceName = null;
	
	private SensorManager mSensorManager;
	private WindowManager mWindowManager;
	private Display mDisplay;
	private ControllerView cv;
	private TextView xDis;
	private TextView dirView;
	private Button forward;
	private Button reverse;
	
	private int direction = STRAIGHT;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        new AsyncTaskSocketServer().execute(Integer.parseInt("8080"));
        
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get an instance of the WindowManager
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        
        // the display for the accelerometer reading
        xDis = (TextView)findViewById(R.id.textView1);
        dirView = (TextView)findViewById(R.id.textView2);
        
        //register the touch actions for our accelerator buttons
        forward = (Button)findViewById(R.id.button1);
        reverse = (Button)findViewById(R.id.button2);
        forward.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
            	if (event.getAction() == (MotionEvent.ACTION_DOWN)) {
            		forward.setBackgroundColor(Color.GREEN);
            		if(mSerialService.getState() == BluetoothSerialService.STATE_CONNECTED)
    					send("f\n".getBytes());
                	return true;
                } else if (event.getAction() == (MotionEvent.ACTION_MOVE)) {
                	return true;
                } else if(event.getAction() == (MotionEvent.ACTION_UP)) {
                	if(mSerialService.getState() == BluetoothSerialService.STATE_CONNECTED)
    					send("0\n".getBytes());
                	forward.setBackgroundColor(Color.GRAY);
                	return true;
                }
                return false;
            }
        });
        reverse.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
            	if (event.getAction() == (MotionEvent.ACTION_DOWN)) {
            		reverse.setBackgroundColor(Color.GREEN);
            		if(mSerialService.getState() == BluetoothSerialService.STATE_CONNECTED)
    					send("b\n".getBytes());
                	return true;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                	return true;
                } else if(event.getAction() == (MotionEvent.ACTION_UP)) {
                	if(mSerialService.getState() == BluetoothSerialService.STATE_CONNECTED)
    					send("0\n".getBytes());
                	reverse.setBackgroundColor(Color.GRAY);
                	return true;
                }
                return false;

            }
        });
        
        
        // bluetooth configuration
     	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    	if (mBluetoothAdapter == null) {
    		finishDialogNoBluetooth();
     		return;
     	}
        
        mSerialService = new BluetoothSerialService(this, mHandlerBT);
        
        
        cv = new ControllerView(this);
        cv.startRun();       
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        mMenuItemConnect = menu.getItem(0);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_connect:
        	
        	if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
        		// Launch the DeviceListActivity to see devices and do scan
        		Intent serverIntent = new Intent(this, DeviceListActivity.class);
        		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        	}
        	else
            	if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
            		mSerialService.stop();
		    		mSerialService.start();
            	}
            return true;
        }
        return false;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        cv.startRun();
        
        /*if (mSerialService != null) {
	    	// Only if the state is STATE_NONE, do we know that we haven't started already
	    	if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
	    		// Start the Bluetooth chat services
	    		mSerialService.start();
	    	}
	    }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        cv.endRun();
    }
    
    public int getConnectionState() {
		return mSerialService.getState();
	}
    
    public void send(byte[] out) {
    	mSerialService.write( out );
    }
    
    private final Handler mHandlerBT = new Handler() {
        @Override
        public void handleMessage(Message msg) {        	
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
                	if (mMenuItemConnect != null) {
                		mMenuItemConnect.setTitle(R.string.disconnect);
                	}
                	String con = "Connected to ";
                	con.concat(mConnectedDeviceName);
                	
                    Toast.makeText(getApplicationContext(), con, Toast.LENGTH_SHORT).show();
                    break;
                    
                case BluetoothSerialService.STATE_CONNECTING:
                	Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
                    break;
                    
                case BluetoothSerialService.STATE_LISTEN:
                case BluetoothSerialService.STATE_NONE:
                	if (mMenuItemConnect != null) {
                		mMenuItemConnect.setTitle(R.string.connect);
                	}
                	
                	Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();

                    break;
                }
                break;
            case MESSAGE_WRITE:
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    public void finishDialogNoBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_no_bt)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setTitle(R.string.app_name)
        .setCancelable( false )
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       finish();            	
                	   }
               });
        AlertDialog alert = builder.create();
        alert.show(); 
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(LOG_TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        
        case REQUEST_CONNECT_DEVICE:

            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mSerialService.connect(device);                
            }
            break;

        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Log.d(LOG_TAG, "BT not enabled");
                
                finishDialogNoBluetooth();                
            }
        }
    }
    
    public void turnLeft() {
    	if(direction == LEFT) {
    		return;
    	} else {
    		if(mSerialService.getState() == BluetoothSerialService.STATE_CONNECTED)
				send("l\n".getBytes());
        	direction = LEFT;
    	}
    	return;
    }
    
    public void turnRight() {
    	if(direction == RIGHT) {
    		return;
    	} else {
    		if(mSerialService.getState() == BluetoothSerialService.STATE_CONNECTED)
				send("r\n".getBytes());
        	direction = RIGHT;
    	}
    	return;
    }
    
    public void goStraight() {
    	if(direction == STRAIGHT) {
    		return;
    	} else {
    		if(mSerialService.getState() == BluetoothSerialService.STATE_CONNECTED)
				send("s\n".getBytes());
        	direction = STRAIGHT;
    	}
    	return;
    }
    
    class ControllerView implements SensorEventListener {
    	private float mSensorX;

        private Sensor mAccelerometer;

    	
    	public ControllerView(Context context) {

            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
        }
    	
    	public void startRun() {
            /*
             * It is not necessary to get accelerometer events at a very high
             * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
             * automatic low-pass filter, which "extracts" the gravity component
             * of the acceleration. As an added benefit, we use less power and
             * CPU resources.
             */
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        public void endRun() {
            mSensorManager.unregisterListener(this);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;
            /*
             * record the accelerometer data, the event's timestamp as well as
             * the current time. The latter is needed so we can calculate the
             * "present" time during rendering. In this application, we need to
             * take into account how the screen is rotated with respect to the
             * sensors (which always return data in a coordinate space aligned
             * to with the screen in its native orientation).
             */

            switch (mDisplay.getRotation()) {
                case Surface.ROTATION_0:
                    mSensorX = event.values[0];
                    break;
                case Surface.ROTATION_90:
                    mSensorX = -event.values[1];
                    break;
                case Surface.ROTATION_180:
                    mSensorX = -event.values[0];
                    break;
                case Surface.ROTATION_270:
                    mSensorX = event.values[1];
                    break;
            }
            xDis.setText(Float.toString(mSensorX));
            
            if(mSensorX > 4.0f) {
            	turnLeft();
            	dirView.setText(R.string.left);
            } else if(mSensorX < -4.0f) {
            	turnRight();
            	dirView.setText(R.string.right);
            } else {
            	goStraight();
            	dirView.setText(R.string.straight);
            }
            
        }
    	
    	@Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}
