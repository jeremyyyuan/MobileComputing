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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.opencsv.CSVWriter;

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
    private List<String[]> outputData = new ArrayList<>();
    private String dataInputPath;
    private int index = 0;
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
    private int collectionInterval = 50000000;
    /*
    public StartSession(SensorManager mSensorManager) {
        this.mSensorManager = mSensorManager;
    }
    */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPython();
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
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void endSession(View view) {
        outputCSV();
        // Do something in response to button
        Intent intent = new Intent(this, EndSession.class);

        TextView timeView = findViewById(R.id.time_view);
        String message = timeView.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);

        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void outputCSV() {
        String filename = "session-data.csv";
        String filepath = "data-dir";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File externalFile = new File(getExternalFilesDir(filepath), filename);
            dataInputPath = externalFile.getPath();
            Log.w("OutputPath", dataInputPath.toString());
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(externalFile);
                CSVWriter writer = new CSVWriter(
                        new OutputStreamWriter(fos, StandardCharsets.UTF_8),
                        ';',
                        CSVWriter.DEFAULT_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END
                );

                //Write the record to file
                writer.writeAll(outputData);
                writer.close();
                Log.w("Output", "output csv succcesful");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String output = runPythonScript();
            Log.w("PickupNum", output);
            final TextView pickupView
                    = findViewById(
                    R.id.pickup_num);

            pickupView.setText(output);
            Log.w("TextView", output);
        }
    }

    private void initPython() {
        if(!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }
    private String runPythonScript() {
        Python python = Python.getInstance();
        PyObject pythonFile = python.getModule("project1");
        return pythonFile.callAttr("classifyData", dataInputPath).toString();
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

                Log.w("SensorAccel", String.valueOf(xAccel));
                Log.w("SensorAccel", String.valueOf(yAccel));
                Log.w("SensorAccel", String.valueOf(zAccel));

                index += 1;
                String[] row = new String[]{String.valueOf(index), String.valueOf(xAccel),
                        String.valueOf(yAccel), String.valueOf(zAccel)};
                outputData.add(row);

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