package com.augmate.BeaconRange.activities;

/**
 * Created by darien on 8/18/14.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.estimote.sdk.Beacon;
import com.augmate.BeaconRange.BeaconParseManager;
import com.augmate.BeaconRange.Beaconizer;
import com.augmate.BeaconRange.IReceiveBeaconsCallbacks;
import com.augmate.BeaconRange.R;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends Activity implements IReceiveBeaconsCallbacks {
    private static final int REQUEST_CODE = 10;
    final double beaconCutoffDist = 3;
    Beaconizer beaconManager;
    BeaconParseManager parseManager;
    private ArrayList<ImageView> beaconImages = new ArrayList<ImageView>();
    private ImageView rightImage1, centerImage1, leftImage1;
    private TextView status;
    private ArrayList<Beacon> validBeacons = new ArrayList<Beacon>();
    private ArrayList<Beacon> discoveredBeacons = new ArrayList<Beacon>();
    private ArrayList<Beacon> removedBeacons = new ArrayList<Beacon>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.show_beacons);
        Collections.addAll(beaconImages, (ImageView) findViewById(R.id.rightImageView1),
                (ImageView) findViewById(R.id.centerImageView1),
                (ImageView) findViewById(R.id.leftImageView1));
        rightImage1 = beaconImages.get(0);
        centerImage1 = beaconImages.get(1);
        leftImage1 = beaconImages.get(2);
        status = (TextView) findViewById(R.id.status);
        beaconManager = new Beaconizer(this, this, beaconCutoffDist);
        parseManager = new BeaconParseManager(this, beaconManager);
    }

    public void onReceiveNearbyBeacons(ArrayList<Beacon> Beacons) {
        for (ImageView i : beaconImages) i.setVisibility(View.INVISIBLE);
        categorizeBeacons(Beacons);
        ((TextView) findViewById(R.id.debug)).setText("Removed: " + removedBeacons.size() + "\n"
                + "Discovered: " + discoveredBeacons.size() + "\n"
                + "Valid: " + validBeacons.size());
        parseManager.put(discoveredBeacons, validBeacons);
        displayBeacons(validBeacons);
    }

    private void categorizeBeacons(ArrayList<Beacon> newValidBeacons) {
        discoveredBeacons.clear();
        removedBeacons.clear();
        for (Beacon b : newValidBeacons) { //loop over all the Beacons from the previous scan
            if (!validBeacons.contains(b))
                discoveredBeacons.add(b); //Else if a beacon from the current scan is NOT in the previous scan, add it to discovered beacons
        }
        validBeacons.removeAll(newValidBeacons); //remove all Beacons in the current scan from the previous to obtain all beacons not found in the current scan.
        removedBeacons = validBeacons; //add these expired beacons to the removedBeacons
        validBeacons = newValidBeacons; //ValidBeacons is a combination of consistent and discovered beacons (ie. newValidBeacons)
    }

    private void displayBeacons(ArrayList<Beacon> validBeacons) {
        switch (validBeacons.size()) {
            case 0:
                status.setText("No beacons detected. Searching...");
                break;
            case 1:
                toggleBeaconIcon(validBeacons.get(0), centerImage1);
                status.setText(beaconManager.getBeaconColor(validBeacons.get(0)) + " beacon detected");
                break;
            case 2:
                toggleBeaconIcon(validBeacons.get(0), leftImage1);
                toggleBeaconIcon(validBeacons.get(1), rightImage1);
                status.setText("You're near a " + beaconManager.getBeaconColor(validBeacons.get(0)) + " beacon and a " +
                        beaconManager.getBeaconColor(validBeacons.get(1)) + " beacon");
                break;
            case 3:
                toggleBeaconIcon(validBeacons.get(0), leftImage1);
                toggleBeaconIcon(validBeacons.get(1), rightImage1);
                toggleBeaconIcon(validBeacons.get(2), centerImage1);
                status.setText("You're near a " + beaconManager.getBeaconColor(validBeacons.get(0)) + " beacon, a " +
                        beaconManager.getBeaconColor(validBeacons.get(1)) + " beacon, and a " + beaconManager.getBeaconColor(validBeacons.get(2)) + " beacon");
                break;
            default:
                toggleBeaconIcon(validBeacons.get(0), leftImage1);
                toggleBeaconIcon(validBeacons.get(1), rightImage1);
                toggleBeaconIcon(validBeacons.get(2), centerImage1);
                status.setText("More than 3 beacons in the area");
                break;
        }
    }

    private void toggleBeaconIcon(Beacon b, ImageView imageView) {
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageResource(beaconManager.getBeaconImage(b));
    }

    public boolean onKeyDown(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Who are you looking for?");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, REQUEST_CODE);
            return true;
        }
        super.onKeyDown(keycode, event);
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            parseManager.findUser(matches.get(0));
        }
        beaconManager.startScanning();
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.destroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        beaconManager.stopScanning();
    }
}
