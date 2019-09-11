package com.example.user.project_app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;


import com.example.user.project_app.trips.TripLog;
import com.example.user.project_app.trips.TripRecord;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.DtcNumberCommand;
import com.github.pires.obd.commands.control.TroubleCodesCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.fuel.FindFuelTypeCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
//import com.github.pires.obd.commands.protocol.CloseCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.google.inject.Inject;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import roboguice.RoboGuice;
import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_obd)
public class ObdActivity extends RoboActivity {

    Integer maxSpeed=0,maxRpm=0;
    String tr_code="";

    String deviceAddress;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice device;
    BluetoothSocket Socket;
    ConnectThread mConnectThread;
    Result_OBD mResult_OBD;

    ArrayList<String> deviceStrs = new ArrayList<String>();
    final ArrayList<String> devices = new ArrayList<String>();
    public static final int BLUETOOTH_REQUEST_CODE = 654;

    /// the trip log
    private TripLog triplog;
    private TripRecord currentTrip;

    private boolean mResult = true;
    private boolean socketResult = true;
    private boolean cykl = false;

    private static final String TAG = ObdActivity.class.getName();
    private static final int CONFIGURATION_IS_OK = 0;
    private static final int DISCONNECT = 1;
    private static final int CONFIGURATION_BAD = 3;
    private static final int DONT_CONNECT = 4;
    private static final int START_CONFIGURATION = 5;
    private static final int OBD_COMMAND_FAILURE = 6;
    private static final int OBD_COMMAND_RPM = 2;
    private static final int OBD_COMMAND_SPEED = 7;
    private static final int OBD_COMMAND_TR_CODE = 8;
    private static final int OBD_COMMAND_DIAGNOSTIC = 9;
    private static final int CONNECT = 10;

    Handler handler = new Handler();

    private Handler mHandler = new Handler(new Handler.Callback() {


        public boolean handleMessage(Message msg) {
            //Log.d(TAG, "Message received on handler");
            switch (msg.what) {
                case CONFIGURATION_IS_OK:
                    makeToast("Configuration is OK!");
                    break;
                case CONFIGURATION_BAD:
                    makeToast("Error configuration");
                    break;
                case START_CONFIGURATION:
                    makeToast("Start Configuration...");
                    break;
                case DISCONNECT:
                    makeToast("Disconnect");
                    break;
                case DONT_CONNECT:
                    makeToast("Don't connect");
                    break;
                case OBD_COMMAND_FAILURE:
                    makeToast("Error OBD I/O Stream");
                    break;
                case OBD_COMMAND_RPM:
                    makeToast("RPM Error");
                    break;
                case OBD_COMMAND_SPEED:
                    makeToast("Speed Error");
                    break;
                case OBD_COMMAND_TR_CODE:
                    makeToast("Trouble code Error");
                    break;
                case OBD_COMMAND_DIAGNOSTIC:
                    makeToast("Diagnostic trouble code Error");
                    break;
                case CONNECT:
                    makeToast("Connect!");
                    break;
            }
            return false;
        }
    });

    public void makeToast(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    static {
        RoboGuice.setUseAnnotationDatabases(false);
    }
    @InjectView(R.id.speedobd)
    TextView speedobd;
    @InjectView(R.id.rpm)
    TextView rpm;
    @InjectView(R.id.result)
    TextView result;

    @Inject
    private SharedPreferences prefs;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        triplog = TripLog.getInstance(this.getApplicationContext());

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            makeToast("Please turn ON your GPS!");
        }
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                location.getLatitude();
                int speed=(int) ((location.getSpeed()*3600)/1000);
                if(speed!=0){
                    speedobd.setText("Speed: "+Integer.toString(speed)+" km/h");
                    if(maxSpeed<speed){
                        maxSpeed=speed;
                    }
                }else{
                    speedobd.setText("Speed: 0 km/h");
                }
            }

            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0, locationListener);

        /* speedobd = (TextView) findViewById(R.id.speedobd);
        rpm = (TextView) findViewById(R.id.rpm);
        result = (TextView) findViewById(R.id.result);
        time = (TextView) findViewById(R.id.time);
        setContentView(R.layout.activity_obd);*/
    }



    private void buildAlertMessageNoBluetooth() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your Bluetooth seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BLUETOOTH_REQUEST_CODE);

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        mResult = false;
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BLUETOOTH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                findYourDevice();
            } else {
                mResult = false;
            }
        }
    }

    private boolean bluetoothOn() {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),
                    "Your phone don't have Bluetooth adapter", Toast.LENGTH_LONG).show();
            return false;
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                buildAlertMessageNoBluetooth();
                return false;
            }
            return true;
        }
    }

    private void findYourDevice() {

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);
        btAdapter.startDiscovery();

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        ProgressDialog dialog;

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                dialog = new ProgressDialog(ObdActivity.this);
                dialog.setTitle("Searching Bluetooth devices...");
                dialog.setMessage("Please wait");
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                dialog.show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                connectDevices();
                dialog.dismiss();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                deviceStrs.add(device.getName() + "\n" + device.getAddress());
                devices.add(device.getAddress());

            }
        }
    };

    public void connectDevices() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice,
                deviceStrs.toArray(new String[deviceStrs.size()]));

        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                deviceAddress = devices.get(position);

                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                device = btAdapter.getRemoteDevice(deviceAddress);


                mConnectThread=new ConnectThread(device);
                mConnectThread.start();

            }
        });

        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();
    }



    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                //Log.e(TAG, "Socket's create() method failed", e);

            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
                mHandler.obtainMessage(CONNECT).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                   // Log.e(TAG, "Could not close the client socket", closeException);

                }
                return;
            }
            Socket=mmSocket;
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                //Log.e(TAG, "Could not close the client socket", e);

            }
        }
    }


     private class Result_OBD extends Thread{

         private final BluetoothSocket socket;
         private boolean conn=true;
         final String protocol = prefs.getString(MyConfigActivity.PROTOCOLS_LIST_KEY, "AUTO");

         public Result_OBD(BluetoothSocket s) {

             socket=s;

             if (socket != null) {
                 if (socket.isConnected()) {

                     mHandler.obtainMessage(START_CONFIGURATION).sendToTarget();
                     try {
                         new ObdResetCommand().run(socket.getInputStream(), socket.getOutputStream());
                         new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                         new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                         new TimeoutCommand(255).run(socket.getInputStream(), socket.getOutputStream());//max255
                         new SelectProtocolCommand(ObdProtocols.valueOf(protocol)).run(socket.getInputStream(), socket.getOutputStream());
                         //new SelectProtocolCommand(ObdProtocols.ISO_14230_4_KWP_FAST).run(socket.getInputStream(), socket.getOutputStream());

                        ///?????????????????????????
                       /* ModifiedTroubleCodesObdCommand tcoc = new ModifiedTroubleCodesObdCommand();
                         tcoc.run(socket.getInputStream(), socket.getOutputStream());*/


                         mHandler.obtainMessage(CONFIGURATION_IS_OK).sendToTarget();
                     }catch (Exception e){
                         conn=false;
                         closeSocket(socket);
                         mHandler.obtainMessage(CONFIGURATION_BAD).sendToTarget();
                     }finally {
                         try {
                             Thread.sleep(1000);
                         } catch (InterruptedException e) {
                             e.printStackTrace();
                         }

                     }

                 }
             }


         }


         public void run() {

             if(conn==true) {

                 while(cykl) {
                     try {
                         RPMCommand engineRpmCommand = new RPMCommand();
                         Thread.sleep(100);
                         engineRpmCommand.run(socket.getInputStream(), socket.getOutputStream());
                         final String RPM =engineRpmCommand.getCalculatedResult();
                         if(Integer.parseInt(RPM)>300){
                         handler.post(new Runnable() {
                             public void run() {
                                 rpm.setText("RPM: "+RPM);
                             }
                         });
                         if(maxRpm<Integer.parseInt(RPM)){
                             maxRpm=Integer.parseInt(RPM);
                         }
                         }
                     } catch (Exception e) {
                         e.printStackTrace();
                     }
                    /*
                     try {
                         SpeedCommand speedCommand = new SpeedCommand();
                         Thread.sleep(100);
                         speedCommand.run(socket.getInputStream(), socket.getOutputStream());
                         final String SPEED = "Speed: " + speedCommand.getCalculatedResult();
                         handler.post(new Runnable() {
                             public void run() {
                                 speedobd.setText(SPEED+" km/h");
                             }
                         });
                     } catch (Exception e) {
                         e.printStackTrace();
                     }*/

                     try {
                         TroubleCodesCommand trouble_code = new TroubleCodesCommand();
                         Thread.sleep(100);
                         trouble_code.run(socket.getInputStream(), socket.getOutputStream());
                         final String TR_Code =  trouble_code.getFormattedResult();

                         if(!TR_Code.equals("")){
                             tr_code+=TR_Code;
                             handler.post(new Runnable() {
                             public void run() {
                                 String res = result.getText().toString();
                                 result.setText(res + "\n" +"Trouble code: "+TR_Code);
                             }
                         });}
                     } catch (Exception e) {
                         e.printStackTrace();
                     }
                 }
             }else{

                 mHandler.obtainMessage(DONT_CONNECT).sendToTarget();
             }
         }


         public void cancel() {
             try {
                 socket.close();
             } catch (IOException e) {

             }
         }

     }


    public void closeSocket(BluetoothSocket sock) {
        if (sock != null)
            // close socket
            try {
                sock.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothOn()) {
          if (Socket != null) {
                if (Socket.isConnected()) {
                    makeToast("Connect!");
                }
          }else{
              findYourDevice();
          }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        deviceStrs.clear();
        devices.clear();
    }

    @Override
    public void onDestroy() {
        cykl=false;
        if (mResult) {
           // unregisterReceiver(mReceiver);

            if (!socketResult) {
                unregisterReceiver(mReceiver);
                mConnectThread.cancel();
                mResult_OBD.cancel();
            }
        }
        super.onDestroy();
    }

    private static final int START_LIVE_DATA = 2;
    private static final int STOP_LIVE_DATA = 3;
    private static final int SETTINGS = 4;
    private static final int TRIPS_LIST = 1;


    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, START_LIVE_DATA, 0, getString(R.string.menu_start_live_data));
        menu.add(0, STOP_LIVE_DATA, 0, getString(R.string.menu_stop_live_data));
        menu.add(0, TRIPS_LIST, 0, getString(R.string.menu_trip_list));
        menu.add(0, SETTINGS, 0, getString(R.string.menu_settings));
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem startItem = menu.findItem(START_LIVE_DATA);
        MenuItem stopItem = menu.findItem(STOP_LIVE_DATA);

        if(!cykl){
            startItem.setEnabled(true);
            stopItem.setEnabled(false);
        }else{
            startItem.setEnabled(false);
            stopItem.setEnabled(true);
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case START_LIVE_DATA:


                if (bluetoothOn()) {
                    if (Socket != null) {
                        if (Socket.isConnected()) {
                            cykl=true;
                            currentTrip = triplog.startTrip();
                            mResult_OBD = new Result_OBD(Socket);
                            mResult_OBD.start();
                        } else {
                            try {
                                Socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            makeToast("Disconnect");
                            findYourDevice();
                        }
                    } else {
                        makeToast("Don't connect");
                        findYourDevice();
                    }
                }
                return true;
            case STOP_LIVE_DATA:
                if (Socket != null) {
                     mResult_OBD.cancel();}

                    cykl=false;

                if (currentTrip != null) {
                    currentTrip.setSpeedMax(Integer.toString(maxSpeed));
                    currentTrip.setEngineRpmMax(Integer.toString(maxRpm));
                    currentTrip.setEngineRuntime(tr_code);
                    currentTrip.setEndDate(new Date());
                    triplog.updateRecord(currentTrip);
                }


                return true;
            case SETTINGS:
                startActivity(new Intent(this, MyConfigActivity.class));
                return true;
            case TRIPS_LIST:
                startActivity(new Intent(this, TripListActivity.class));
                return true;
        }
        return false;
    }

}
