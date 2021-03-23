package com.example.studybuddy;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.opencsv.CSVWriter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.StringJoiner;

public class StartSession extends AppCompatActivity implements SensorEventListener {

    enum Activity {
        DEVICE_MOVEMENT,
        DEVICE_STATIONARY,
        APP_FOREGROUND,
        APP_BACKGROUND
        // NOTIFICATION
    }
    public static class Event<Type, Time> {
        public final Type t;
        public final Time s;
        public Event(Type t, Time s) {
            this.t = t;
            this.s = s;
        }
    }
    /*
    public static class AccelEvent<Time, XValue, YValue, ZValue> {
        public final Time t;
        public final XValue x;
        public final YValue y;
        public final ZValue z;
        public AccelEvent(Time t, XValue x, YValue y, ZValue z) {
            this.t = t;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    public ArrayList<AccelEvent<Integer, Float, Float, Float>> accelEvents = new ArrayList<>();
    */
    private ArrayList<String> outputData = new ArrayList<>();
    // Initialize list of events for output
    ArrayList<Event<Activity, Integer>> events = new ArrayList<>();

    // Number of seconds displayed
    // on the stopwatch.
    private int seconds = 0;

    // Number of distractions displayed
    // on the start screen - indicates
    // the amount of time the user picks switches apps
    public static int app_distractions = 0;

    // Indicates how many times the user picked up the phone
    public static int device_pickups = 0;


    // Is the stopwatch running?
    private boolean running = false;
    // Is the phone in motion?
    private boolean moving = false;

    public static final String EXTRA_MESSAGE = "00:00:00";

    // Sensors
    private SensorManager mSensorManager;
    private Sensor mSensorAccel;
    // private int collectionInterval = 50000000;
    /*
    public StartSession(SensorManager mSensorManager) {
        this.mSensorManager = mSensorManager;
    }
    */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_session);

        String currentDate = new SimpleDateFormat("MMM, d yyyy", Locale.getDefault()).format(new Date());
        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.date);
        textView.setText(currentDate);

        running = true;
        runTimer();

        /* Begin monitoring accelerometer sensors */
        monitorDevicePickups();

        /* Begin monitoring phone distractions */
        monitorAppDistractions();
    }

    /** Called when the user taps the Start Session button */
    public void endSession(View view) {

        // Print values
        try {
            outputToCSV();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Do something in response to button
        Intent intent = new Intent(this, EndSession.class);

        TextView timeView = findViewById(R.id.time_view);
        String message = timeView.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);

        startActivity(intent);
    }

    private void outputToCSV() throws IOException {

        String filename = "session-data.csv";
        String filepath = "data-dir";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.w("CSV", "Storage is mounted");
        } else if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)){
            Log.w("CSV", "Storage is read only");
        }
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(filename, MODE_PRIVATE);

            try (CSVWriter w = new CSVWriter(new FileWriter(String.valueOf(fos)))) {
                String[] record = "4,David,Miller,Australia,30".split(",");
                //Write the record to file
                w.writeNext(record);

                //close the writer
                fos.close();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }




    }

    // Sets up accelerometer sensor data collection
    // to determine device pickups
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void monitorDevicePickups() {




       // Log.w("SensorDebug", "sensor manager already initialized");
        mSensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mSensorAccel =
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (mSensorAccel == null) {
            Log.w("SensorAccel", "no sensor");
        }
        if(mSensorAccel != null) {
            mSensorManager.registerListener(this, mSensorAccel, SensorManager.SENSOR_DELAY_NORMAL);
            Log.w("SensorDebug", "Initializing sensor manager");
        }

    }

    // TODO: Might need to remove and implement in oncreate so sensor data is
    //  running in the background
    /*
    @Override
    protected void onStart() {
        super.onStart();

        if (mSensorAccel != null) {
            mSensorManager.registerListener(this, mSensorAccel,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorAccel != null) {
            mSensorManager.registerListener(this, mSensorAccel,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
     */
    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
        Log.w("SensorDebug", "stopped sensor pickup");
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("StringFormatMatches")
    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.w("SensorDebug", "sensor event detected");
        if (running) {
            int sensorType = event.sensor.getType();

            if (sensorType == Sensor.TYPE_ACCELEROMETER) {
                float xAccel = event.values[0];
                float yAccel = event.values[1];
                float zAccel = event.values[2];

                StringJoiner joiner = new StringJoiner(",");
                joiner.add(String.valueOf(seconds)).add(String.valueOf(xAccel))
                    .add(String.valueOf(yAccel))
                    .add(String.valueOf(zAccel));
                String line = joiner.toString();
                outputData.add(line);
                // accelEvents.add(new AccelEvent<>(seconds, xAccel, yAccel, zAccel));

                Log.w("SensorAccel", String.valueOf(xAccel));
                Log.w("SensorAccel", String.valueOf(yAccel));
                Log.w("SensorAccel", String.valueOf(zAccel));

                Activity a;
                if (moving) {
                    if (xAccel == 0 && yAccel == 0.81 && zAccel == 9.78) {
                        a = Activity.DEVICE_STATIONARY;
                        events.add(new Event<>(a, seconds));
                        moving = false;
                    }
                    // Ignore if the movement is continuous
                } else {
                    a = Activity.DEVICE_MOVEMENT;
                    events.add(new Event<>(a, seconds));
                    device_pickups += 1;
                    moving = true;
                    // Update the text view.
                    final TextView pickupView
                            = findViewById(
                            R.id.pickup_num);
                    pickupView.setText(String.valueOf(device_pickups));
                    Log.w("TextView", String.valueOf(device_pickups));
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Calculate how many times left the app
    private void monitorAppDistractions() {
        // Reset the text view.
        final TextView distractionView
                = findViewById(
                R.id.distraction_num);
        app_distractions = 0;
        distractionView.setText(String.valueOf(app_distractions));
        Activity a = Activity.APP_FOREGROUND;
        events.add(new Event<>(a, seconds));

    }
    @Override
    protected void onPause() {
        super.onPause();
        // When app moves to background increase distraction count
        if (running) {
            app_distractions += 1;
            // Log activity
            Log.w("DataCollection", "App moved to background");

            Activity a = Activity.APP_BACKGROUND;
            events.add(new Event<>(a, seconds));
            // Update the text view.
            final TextView distractionView
                    = findViewById(
                    R.id.distraction_num);
            distractionView.setText(String.valueOf(app_distractions));
        }
    }

    protected void onResume() {
        super.onResume();
        // Log activity
        Log.w("DataCollection", "App moved to foreground");

        Activity a = Activity.APP_FOREGROUND;
        events.add(new Event<>(a, seconds));
    }

    // Sets the NUmber of seconds on the timer.
    // The runTimer() method uses a Handler
    // to increment the seconds and
    // update the text view.
    private void runTimer()
    {
        // Get the text view.
        final TextView timeView
                = findViewById(
                R.id.time_view);

        // Creates a new Handler
        final Handler handler
                = new Handler();

        // Call the post() method,
        // passing in a new Runnable.
        // The post() method processes
        // code without a delay,
        // so the code in the Runnable
        // will run almost immediately.
        handler.post(new Runnable() {
            @Override

            public void run()
            {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;

                // Format the seconds into hours, minutes,
                // and seconds.
                String time
                        = String
                        .format(Locale.getDefault(),
                                "%d:%02d:%02d", hours,
                                minutes, secs);

                // Set the text view text.
                timeView.setText(time);

                // If running is true, increment the
                // seconds variable.
                if (running) {
                    seconds++;
                }

                // Post the code again
                // with a delay of 1 second.
                handler.postDelayed(this, 1000);
            }
        });
    }

}