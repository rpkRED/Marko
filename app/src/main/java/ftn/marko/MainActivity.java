package ftn.marko;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 2;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int MESSAGE_READ = 1;
    private static final int SUCCESS_CONNECTED = 0;
    private final int TYPED_IN = 2;
    protected static final int TOUCHED = 5;
    public static Handler handler;
    static Intent btIntent;
    Button connectBtn;
    protected static final String LOG_TAG = "MainActivity";
    static BluetoothAdapter adapterBT;
    static BluetoothDevice selectedDevice;
    private ConnectThread connect;
    private ConnectedThread connected;
    String vraceno;
    private boolean connectionFlag = false;
    public Bitmap bitmap;
    TextView textView1, textView2, textView3, textView4, textView5;
    RelativeLayout layout_joystick;
    Joystick js;
    static boolean selektovanBT = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        Log.d(LOG_TAG, "OnCreate");
        checkBT();
        Init();
        handler = new Handler(new Handler.Callback() {

            @Override
            public boolean handleMessage(Message msg) {

                connectBtn.setText(R.string.disconnect_btn);
                connectBtn.setVisibility(View.VISIBLE);
                switch (msg.what) {

                    case SUCCESS_CONNECTED:
                        connected = new ConnectedThread((BluetoothSocket) msg.obj);
                        connected.start();
                        break;

                    case MESSAGE_READ:
                        vraceno = new String((byte[]) msg.obj);
                        break;

                    case TYPED_IN:
                        String a = (String) msg.obj;
                        String b = a.concat("\r\n");
                        Log.d(LOG_TAG,b);
                        connected.write(b.getBytes());
                        break;

                    case TOUCHED:
                        double[] db =(double[]) msg.obj;
                        a = "H" + String.valueOf(db[0]) + "M" + String.valueOf(db[1]) + "E";
                        b = a.concat("\r\n");
                        connected.write(b.getBytes());

                        break;
                }
                return true;
            }
        });
        layout_joystick = (RelativeLayout)findViewById(R.id.layout_joystick);
//      Joystick za mobilni
        js = new Joystick(getApplicationContext(), layout_joystick, R.mipmap.ball);
        js.setStickSize(50, 50);
        js.setLayoutSize(250, 250);
        js.setLayoutAlpha(150);
        js.setStickAlpha(100);
        js.setOffset(js.getStickHeight()/2);
        js.setMinimumDistance(10);
//      Joystick za tablet
//        js = new Joystick(getApplicationContext(), layout_joystick, R.mipmap.ball);
//        js.setStickSize(200, 200);
//        js.setLayoutSize(1000, 1000);
//        js.setLayoutAlpha(150);
//        js.setStickAlpha(100);
//        js.setOffset(js.getStickHeight()/2);
//        js.setMinimumDistance(50);


        layout_joystick.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js.drawStick(arg1);
                js.calculate8();
                if (arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                    textView1.setText("X : " + String.valueOf(js.getX()));
                    textView2.setText("Y : " + String.valueOf(js.getY()));
                    textView3.setText("Angle : " + String.valueOf(js.getAngle()));
                    textView4.setText("Distance : " + String.valueOf(js.getDistance()));
                    if (connectionFlag == true) {
                        handler.obtainMessage(TOUCHED, js.porukaMotori).sendToTarget();
                    }
                    int direction = js.get8Direction();
                    if (direction == Joystick.STICK_UP) {
                        textView5.setText("Direction : Up");
                    } else if (direction == Joystick.STICK_UPRIGHT) {
                        textView5.setText("Direction : Up Right");
                    } else if (direction == Joystick.STICK_RIGHT) {
                        textView5.setText("Direction : Right");
                    } else if (direction == Joystick.STICK_DOWNRIGHT) {
                        textView5.setText("Direction : Down Right");
                    } else if (direction == Joystick.STICK_DOWN) {
                        textView5.setText("Direction : Down");
                    } else if (direction == Joystick.STICK_DOWNLEFT) {
                        textView5.setText("Direction : Down Left");
                    } else if (direction == Joystick.STICK_LEFT) {
                        textView5.setText("Direction : Left");
                    } else if (direction == Joystick.STICK_UPLEFT) {
                        textView5.setText("Direction : Up Left");
                    } else if (direction == Joystick.STICK_NONE) {
                        textView5.setText("Direction : Center");
                    }
                } else if (arg1.getAction() == MotionEvent.ACTION_UP) {
                    textView1.setText("X :");
                    textView2.setText("Y :");
                    textView3.setText("Angle :");
                    textView4.setText("Distance :");
                    textView5.setText("Direction :");
                }
                return true;
            }
        });
    }

    private void startConnect() {
        if (selectedDevice != null) {
            connectionFlag = true;
            connect = new ConnectThread(selectedDevice);
            connect.start();
        }

    }

    public void btnClickHandler(View view) {

        if (view.getId() == R.id.btnConnect) {

            if (connectBtn.getText().toString().equalsIgnoreCase("connect")) {
                btIntent = new Intent(this, DeviceList.class);
                startActivity(btIntent);
            } else if (connectBtn.getText().toString().equalsIgnoreCase("disconnect")) {
                connected.cancel();
                connect.cancel();
                connectionFlag = false;
                connectBtn.setText(R.string.connect_btn);
                selectedDevice = null;
                Toast.makeText(MainActivity.this, "You are now disconnected!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void Init() {

        selectedDevice = null;
        textView1 = (TextView)findViewById(R.id.textView1);
        textView2 = (TextView)findViewById(R.id.textView2);
        textView3 = (TextView)findViewById(R.id.textView3);
        textView4 = (TextView)findViewById(R.id.textView4);
        textView5 = (TextView)findViewById(R.id.textView5);
        connectBtn = (Button) findViewById(R.id.btnConnect);
        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ball);
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {


                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    if (MainActivity.adapterBT.getState() == MainActivity.adapterBT.STATE_OFF) {
                        turnOnBT();
                        if(connectionFlag){
                            connected.cancel();
                            connect.cancel();
                            connectionFlag = false;
                            connectBtn.setText(R.string.connect_btn);
                            selectedDevice = null;
                        }

                    }
                }
            }
        };
        IntentFilter mFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, mFilter);

    }

    private void checkBT() {

        adapterBT = BluetoothAdapter.getDefaultAdapter();

        if (adapterBT == null) {
            Toast.makeText(this, "Bluetooth not found!", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!adapterBT.isEnabled()) {
            turnOnBT();
        }
    }

    private void turnOnBT() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Bluetooth must be enabled!", Toast.LENGTH_SHORT).show();
                finish();
            }
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "bluetooth is enabled", Toast.LENGTH_SHORT).show();
                finishActivity(REQUEST_ENABLE_BT);
            }
        }
    }



    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {

            BluetoothSocket tmp_socket = null;
            mmDevice = device;

            try {
                tmp_socket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.v(LOG_TAG, "GRESKA kod uspostavljanja socket-a u ConnectThread-u.");
            }

            mmSocket = tmp_socket;

        }

        public void run() {

            adapterBT.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.d(LOG_TAG, "VALJA ZA SADA, odradio sam mmSocet.connect");
            } catch (Exception e) {

                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.v(LOG_TAG, "NEVALJA, nisam odradio mmSocket.connect");
                }

            }

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "YOU ARE NOW CONNECTED", Toast.LENGTH_SHORT).show();

                }
            });

            handler.obtainMessage(SUCCESS_CONNECTED, mmSocket).sendToTarget();
            Log.d(LOG_TAG, "Odradio sam handler.obtainMsg u ConnectThread-u");

        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.d(LOG_TAG, "Nije uspeo da zatvori mmSocet u ConnectThread-u. ");
            }
        }
    }

    private class ConnectedThread extends Thread {

        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothSocket mmSocket;


        public ConnectedThread(BluetoothSocket Socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mmSocket = Socket;

            try {
                tmpIn = Socket.getInputStream();
                tmpOut = Socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            byte[] buffer;
            int bytes;

            while (true) {

                try {
                    buffer = new byte[1024];
                    bytes = mmInStream.read(buffer);
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                try {
                    connected.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(selektovanBT) {
            connectBtn.setVisibility(View.GONE);
            Toast.makeText(this,"CONNECTING, PLEASE WAIT", Toast.LENGTH_SHORT).show();
            startConnect();
            selektovanBT = false;
        }
        Log.d(LOG_TAG, "onResume");

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(LOG_TAG, "onRestart");

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.e(LOG_TAG, "ORIENTATION_LANDSCAPE");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.e(LOG_TAG, "ORIENTATION_PORTRAIT");
        }
    }
}
