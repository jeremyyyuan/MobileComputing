package com.example.studybuddy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class EndSession extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_session);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String message = intent.getStringExtra(StartSession.EXTRA_MESSAGE);

        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.time_view2);
        textView.setText(message);

        updateStatistics();
    }

    /** Called when the user taps the Start Session button */
    public void backToMain(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void updateStatistics() {
        final TextView pickupView
                = findViewById(
                R.id.pickup_num2);
        pickupView.setText(String.valueOf(StartSession.device_pickups));

        final TextView distractionView
                = findViewById(
                R.id.distraction_num2);
        // Set to app_distractions - 1 because clicking End Session actually increments it by one.
        distractionView.setText(String.valueOf(StartSession.app_distractions - 1));
    }
}