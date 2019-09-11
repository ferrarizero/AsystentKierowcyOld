package com.example.user.project_app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

import android.support.v4.app.ActivityCompat;

import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;




public class MainActivity extends Activity {

    TextView speedview;
    SurfaceView surfaceView;
    SurfaceHolder holder;
    Timer timer = new Timer();
    Camera camera;
    MediaRecorder mediaRecorder;
    private Chronometer mChronometer;
    File photoFile;
    Integer audio =0;//on turn audio
    Integer video =0;//max fullHD
    Integer memory =0;//phone memory default
    Integer time =0;//time recording 1 minute default

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
           // Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.READ_LOGS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
                configure();
        }
    }

    @SuppressLint("MissingPermission")
    public void configure(){
        final boolean[] created = {false};

        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mChronometer.setVisibility(View.INVISIBLE);

        speedview=(TextView)findViewById(R.id.speedView);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                location.getLatitude();
                int speed=(int) ((location.getSpeed()*3600)/1000);
                if(speed!=0){
                    speedview.setText(Integer.toString(speed)+" km/h");
                }else{
                    speedview.setText("0 km/h");
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

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        holder = surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    created[0] = true;
                    try {
                        camera = Camera.open();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //camera = Camera.open();
                    Camera.Parameters param = camera.getParameters();
                    param.set("cam_mode", 1);
                    param.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                    camera.setParameters(param);

                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
        if (created[0]==false){
            try {
                camera = Camera.open();
                Camera.Parameters param = camera.getParameters();
                param.set("cam_mode", 1);
                param.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                camera.setParameters(param);

                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        configure();
    }
    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        /*camera = Camera.open();
        Camera.Parameters param = camera.getParameters();
        param.set("cam_mode", 1);
        param.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
        camera.setParameters(param);*/
    }

    @Override
    protected void onPause() {
        super.onPause();
       /* releaseMediaRecorder();
        if (camera != null)
            camera.release();
        camera = null;*/
    }

    public void onClickPicture(View view) {
        camera.takePicture(null, null, new PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd 'at' HH.mm.ss");

                    //File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "Foto_Camcorder");

                    if (!mediaStorageDir.exists()) {
                        if (!mediaStorageDir.mkdirs()) {
                           // Log.d("App", "failed to create directory");
                        }
                    }
                    photoFile = new File(mediaStorageDir, dateFormat.format(new Date())+".jpg");
                    FileOutputStream fos = new FileOutputStream(photoFile);
                    fos.write(data);
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }


    class UpdateTimeTask extends TimerTask {

        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if(isFreeSpace()<1024){//less 1 GB
                        deleteVideo();//delete old video
                    }

                if (mediaRecorder != null) {
                mediaRecorder.stop();
                mChronometer.stop();
                mChronometer.setVisibility(View.INVISIBLE);
                releaseMediaRecorder();
                 }


            if (prepareVideoRecorder()) {
                mediaRecorder.start();
               mChronometer.setVisibility(View.VISIBLE);
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.start();
            } else {
                releaseMediaRecorder();
                 }
                    restartVideo();
                }
            });

        }
    }

    public void restartVideo(){

        TimerTask task=new UpdateTimeTask();
        timer = new Timer();
        timer.purge();
        if(time==0){
            timer.schedule(task,60000); //1 min
        }else if(time==1){
            timer.schedule(task,120000); //2 min
        }else{
            timer.schedule(task,300000); //5 min
        }

    }


    public void onClickStartRecord(View view) {
        if(isFreeSpace()<1024){//less 1 GB
            deleteVideo();//delete old video
        }

        if (prepareVideoRecorder()) {
            mediaRecorder.start();
            mChronometer.setVisibility(View.VISIBLE);
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mChronometer.start();
        } else {
            releaseMediaRecorder();
        }
        restartVideo();
    }

    public void onClickStopRecord(View view) {
        if (mediaRecorder != null) {
            timer.cancel();
            mediaRecorder.stop();
            mChronometer.stop();
            mChronometer.setVisibility(View.INVISIBLE);
            releaseMediaRecorder();
        }
    }

    public void onClickOpenNavigation(View view){
        Intent intent = new Intent(this, NavigationActivity.class);
        startActivity(intent);
    }

    public void onClickOpenOBD(View view){
        Intent intent = new Intent(this, ObdActivity.class);
        startActivity(intent);
    }

    String videoStorageDir;
    private void deleteVideo(){

   // String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
    if(memory==0) {
        videoStorageDir = Environment.getExternalStorageDirectory().toString() + "/Video_Camcorder";
    }else{
        videoStorageDir = Environment.getExternalStorageDirectory().toString() + "/VideoSD_Camcorder";
    }
    File file = new File(videoStorageDir);

    ArrayList<File> files = new ArrayList<File>();
    files=getAllFilesInDir(file);
    Calendar time = Calendar.getInstance();
    time.add(Calendar.DAY_OF_YEAR,-7);

    for (File f : files) {
        if(f.exists()){

            Date lastModified = new Date(f.lastModified());
            if(lastModified.before(time.getTime())) {
                f.delete();//old than 7 days
            }else{
                f.delete();//first save video
                break;
            }

        }
    }

}

    public static ArrayList<File> getAllFilesInDir(File dir) {
        if (dir == null)
            return null;

        ArrayList<File> files = new ArrayList<File>();

        Stack<File> dirlist = new Stack<File>();
        dirlist.clear();
        dirlist.push(dir);

        while (!dirlist.isEmpty()) {
            File dirCurrent = dirlist.pop();

            File[] fileList = dirCurrent.listFiles();
            for (File aFileList : fileList) {
                if (aFileList.isDirectory())
                    dirlist.push(aFileList);
                else
                    files.add(aFileList);
            }
        }

        return files;
    }

    private boolean prepareVideoRecorder() {

        camera.unlock();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(camera);

        if(audio==1) {

            mediaRecorder.setPreviewDisplay(holder.getSurface());
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);


        }else{

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            if (video == 0) {
                mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
            } else if (video == 1) {
                mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
            } else {
                mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
            }

        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd 'at' HH.mm.ss");
        File videoStorageDir = new File(Environment.getExternalStorageDirectory(), "Video_Camcorder");

        if (!videoStorageDir.exists()) {
            if (!videoStorageDir.mkdirs()) {

            }
        }

        if(memory==0) {
            mediaRecorder.setOutputFile(new File(videoStorageDir,dateFormat.format(new Date()) + ".mp4").getAbsolutePath());
            /*mediaRecorder.setOutputFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    ,dateFormat.format(new Date()) + ".mp4").getAbsolutePath());*/
        }else{

            File sdPath = Environment.getExternalStorageDirectory();
            sdPath = new File(sdPath.getAbsolutePath() + "/VideoSD_Camcorder");
            if (!sdPath.exists()) {
                if (!sdPath.mkdirs()) {

                }
            }

            /*mediaRecorder.setOutputFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Rejestrator"
                    ,dateFormat.format(new Date()) + ".mp4").getAbsolutePath());*/
            mediaRecorder.setOutputFile(new File(sdPath,dateFormat.format(new Date()) + ".mp4").getAbsolutePath());

        }

        mediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());
        mediaRecorder.setOrientationHint(90);
        mediaRecorder.setVideoSize(1920, 1080);

        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
        }
    }

    public Float isFreeSpace(){
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long bytesAvailable = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)
                bytesAvailable = (long) stat.getBlockSizeLong() * (long) stat.getAvailableBlocksLong();
            else
                bytesAvailable = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();

          float megAvailable= bytesAvailable / (1024.f * 1024.f);

        return megAvailable;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int video2=video;
        final int audio2=audio;
        final int memory2=memory;
        final int time2=time;

        int id = item.getItemId();
        if (id == R.id.action_name) {
            CharSequence[] array = {"FullHD", "HD", "480p"};
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.video_settings);
            alertDialogBuilder
                    .setCancelable(false)
                    .setSingleChoiceItems(array, video ,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case 0:
                                    video=0;
                                    break;
                                case 1:
                                    video=1;
                                    break;
                                case 2:
                                    video=2;
                                    break;
                                default:
                                    break;

                            }

                        }
                    })
                    .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {

                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            if(video2!=video){
                                video=video2;
                            }

                            dialog.cancel();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        if (id == R.id.action_name2) {
            CharSequence[] array = {"ON", "OFF"};
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.audio_settings);
            alertDialogBuilder
                    .setCancelable(false)
                    .setSingleChoiceItems(array, audio ,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case 0:
                                   audio=0;
                                    break;
                                case 1:
                                    audio=1;
                                    break;

                            }

                        }
                    })
                    .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {

                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            if(audio2!=audio){
                                audio=audio2;
                            }
                            dialog.cancel();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        if (id == R.id.action_name3) {
            CharSequence[] array = {"Phone", "SD card"};
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.memory_settings);
            alertDialogBuilder
                    .setCancelable(false)
                    .setSingleChoiceItems(array,memory ,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case 0:
                                   memory=0;
                                    break;
                                case 1:
                                 memory=1;
                                    Boolean isSDPresent = android.os.Environment.
                                            getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
                                    Boolean isSDSupportedDevice = Environment.isExternalStorageRemovable();
                                    if(!(isSDSupportedDevice && isSDPresent)) {
                                        Toast.makeText(getApplicationContext(),"You don't have sd card!",
                                                Toast.LENGTH_LONG).show();
                                        memory=0;
                                        }

                                    break;

                            }

                        }
                    })
                    .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {

                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            if(memory2!=memory){
                                memory=memory2;
                            }

                            dialog.cancel();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        if (id == R.id.action_name4) {
            CharSequence[] array = {"1 minute", "2 minutes","5 minutes"};
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.time_settings);
            alertDialogBuilder
                    .setCancelable(false)
                    .setSingleChoiceItems(array, time ,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case 0:
                                    time=0;
                                    break;
                                case 1:
                                    time=1;
                                    break;
                                case 2:
                                    time=2;
                                    break;

                            }

                        }
                    })
                    .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {

                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            if(time2!=time){
                                time=time2;
                            }
                            dialog.cancel();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        return super.onOptionsItemSelected(item);
    }

}