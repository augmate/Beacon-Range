package com.example.BeaconRange.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.example.BeaconRange.R;

public class MainActivity extends Activity {
    private String userName = "Darien";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        launchBeaconRangeActivity(userName);
        finish();
    }

    private void launchBeaconRangeActivity(String userName) {
        Intent i = new Intent(this, CheckInRangeActivity.class);
        startActivity(i);
    }
}
