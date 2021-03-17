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
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class StartSession extends AppCompatActivity implements SensorEventListener {

    enum Activity {
        PICKUP,
        STATIONARY,
        DISTRACTION
//        NOTIFICATION
    }
    public class Event<Type, StartTime, EndTime> {
        public final Type t;
        public final StartTime s;
        public final EndTime e;
        public Event(Type t, StartTime s, EndTime  e) {
            this.t = t;
            this.s = s;
            this.e = e;
        }
    }

    // Initialize list of events for output
    ArrayList<Event> events = new ArrayList<>();

    // Number of seconds displayed
    // on the stopwatch.
    private int seconds = 0;

    // Number of distractions displayed
    // on the start screen
    private int distractions = 0;

    // Number of pickups displayed on the screet

    // Is the stopwatch running?
    private boolean running;

    public static final String EXTRA_MESSAGE = "00:00:00";

    // Sensors
    private SensorManager mSensorManager;
    private Sensor mSensorAccel;
    private float previousAccelValue;
    // private TextView mTextSensorAccel;
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
        /* Begin monitoring phone distractions */
        monitorDistractions();
        /* Begin monitoring Sensors */
        monitorSession();
    }

    /** Called when the user taps the Start Session button */
    public void endSession(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, EndSession.class);

        TextView timeView = findViewById(R.id.time_view);
        String message = timeView.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);

        startActivity(intent);
    }

    // Sets up sensor data collection
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void monitorSession() {
        if (mSensorAccel != null) {
            mSensorManager.registerListener(this, mSensorAccel,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        mSensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // mTextSensorAccel = (TextView) findViewById(R.id.label_accel);
        mSensorAccel =
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        /*
            public void onSensorChanged(SensorEvent event) {

            }
        */
        // String sensor_error = getResources().getString(R.string.error_no_sensor);

        if (mSensorAccel == null) {
            // mTextSensorAccel.setText(sensor_error);

            Log.w("SensorAccel", "no sensor");
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
    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
    }
     */

    @SuppressLint("StringFormatMatches")
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (running) {
            float currentValue = event.values[0];
            int sensorType = event.sensor.getType();

            if (sensorType == Sensor.TYPE_ACCELEROMETER) {
           /* mTextSensorAccel.setText(getResources().getString(
                    R.string.accelerometer_sensor_1_2f_2_2f_3_2f, currentValue));
            */
                final int sensorAccel = Log.w("SensorAccel", String.valueOf(currentValue));
                if (previousAccelValue != currentValue) {
                    Activity a = Activity.PICKUP;
                    // Event e = new Event<enum a, int seconds, int seconds>();
                    events.add(new Event(a, seconds, seconds));
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Calculate how many times left the app
    private void monitorDistractions() {

        // Get the text view.
        final TextView distractionView
                = findViewById(
                R.id.distraction_num);
        distractions = 0;
        distractionView.setText(String.valueOf(distractions));

    }
    @Override
    protected void onPause() {
        super.onPause();
        if (running) {
            distractions += 1;
            // Get the text view.
            final TextView distractionView
                    = findViewById(
                    R.id.distraction_num);
            distractionView.setText(String.valueOf(distractions));
        }
    }

    protected void onResume() {
        super.onResume();
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