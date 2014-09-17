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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.augmate.BeaconRange.*;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;

import java.util.ArrayList;

public class MainActivity extends Activity implements IReceiveBeaconsCallbacks {
    private static final int REQUEST_CODE = 10;
    final double beaconCutoffDist = 3;
    Beaconizer beaconManager;
    BeaconParseManager parseManager;
    private ArrayList<ImageView> beaconImages = new ArrayList<ImageView>();
    private CustomList adapter;
    private ListView list;
    private ArrayList<Beacon> validBeacons = new ArrayList<Beacon>();
    private ArrayList<Beacon> discoveredBeacons = new ArrayList<Beacon>();
    private ArrayList<Beacon> removedBeacons = new ArrayList<Beacon>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.show_beacons);
        beaconManager = new Beaconizer(this, this, beaconCutoffDist);
        parseManager = new BeaconParseManager(this, beaconManager);
        list=(ListView)findViewById(R.id.list);
        list.setSelector(android.R.color.transparent);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { //This listener disables clicks on the listview and interprets its as a onKeyDown action
            @Override
            public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
                onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null);
            }
        });
    }

    public void onReceiveNearbyBeacons(ArrayList<Beacon> Beacons) {
        for (ImageView i : beaconImages) i.setVisibility(View.INVISIBLE);
        //TODO Test how application behaves when only one beacon can be owned by a user at a give time
        if(!Beacons.isEmpty()){
            Beacon b = Beacons.get(0);
            Beacons.clear();
            Beacons.add(b);
        }
        boolean nothingChanged = validBeacons.containsAll(Beacons);
        categorizeBeacons(Beacons);
        ((TextView) findViewById(R.id.debug)).setText("Removed: " + removedBeacons.size() + "\n"
                + "Discovered: " + discoveredBeacons.size() + "\n"
                + "Valid: " + validBeacons.size());
        if(!nothingChanged)parseManager.put(discoveredBeacons, validBeacons);
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
        String[] beaconNames = new String[validBeacons.size()];
        Integer[] beaconImages = new Integer[validBeacons.size()];
        for(int i = 0; i < validBeacons.size(); i++){
            final Beacon b = validBeacons.get(i);
            beaconNames[i] = "Color: " +beaconManager.getBeaconColor(b)+ "\n"
                    + "Distance: " + (float) Utils.computeAccuracy(b);
            beaconImages[i] = beaconManager.getBeaconImage(b);
        }
        adapter = new CustomList(MainActivity.this, beaconNames, beaconImages);
        list.setAdapter(adapter);
    }

    public boolean onKeyDown(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Who are you looking for?");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, REQUEST_CODE);
        }
        super.onKeyDown(keycode, event);
        return true;
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
        beaconManager.destroy();
        super.onDestroy();
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
