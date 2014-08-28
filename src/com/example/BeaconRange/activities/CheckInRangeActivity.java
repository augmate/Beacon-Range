package com.example.BeaconRange.activities;

/**
 * Created by darien on 8/18/14.
 */

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.estimote.sdk.Beacon;
import com.example.BeaconRange.BeaconParseManager;
import com.example.BeaconRange.Beaconizer;
import com.example.BeaconRange.IReceiveBeaconsCallbacks;
import com.example.BeaconRange.R;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseUser;

import java.util.*;

public class CheckInRangeActivity extends Activity implements IReceiveBeaconsCallbacks {
    Beaconizer newBeaconManager;
    BeaconParseManager newParseManager;
    final String TAG = "BEACON";
    final String YOUR_APPLICATION_ID = "JdeDnORM2uskVmy91dcJZiWnY8ITPZcBrg2RRNht";
    final String YOUR_CLIENT_KEY = "f8VQFqtfDIh8rvMzMaclTsDoiH6cg9Rf5Tbz2jcZ";
    final double beaconCutoffDist = 3;
    private int beaconThreshold = 6;
    private int thresholdCount = 0;
    private ArrayList<ImageView> beaconImages = new ArrayList<ImageView>();
    private HashMap<Beacon, Integer> map = new HashMap<Beacon, Integer>();
    private ImageView rightImage, centerImage, leftImage;
    private TextView status;
    private ArrayList<Beacon> validBeacons = new ArrayList<Beacon>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parse.initialize(this, YOUR_APPLICATION_ID, YOUR_CLIENT_KEY);
        ParseUser.enableAutomaticUser();
        ParseACL defaultACL = new ParseACL();
        // Optionally enable public read access.
        // defaultACL.setPublicReadAccess(true);
        //ParseACL.setDefaultACL(defaultACL, true);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.check_between_layout);
        Collections.addAll(beaconImages, (ImageView) findViewById(R.id.rightImageView),
                (ImageView) findViewById(R.id.centerImageView),
                (ImageView) findViewById(R.id.leftImageView));
        rightImage = beaconImages.get(0);
        centerImage = beaconImages.get(1);
        leftImage = beaconImages.get(2);
        status = (TextView) findViewById(R.id.status);
        newBeaconManager = new Beaconizer(this, this, beaconCutoffDist);
        newParseManager = new BeaconParseManager(newBeaconManager.minorToBeaconAttrib);
        Log.d(TAG,"BeaconManager configured.");
    }

    public void onReceiveNearbyBeacons(List<Beacon> Beacons) {
        /*
        if(hasSameBeacons(currentBeaconArray, beacons)) {
            thresholdCount += beacons.size();
            if(beacons.size()==0) thresholdCount++;
        }
        else {
            currentBeaconArray = beacons;
            thresholdCount = 0;
        }*/
        for(Beacon b:Beacons){
            Integer value = map.get(b);
            Log.d(TAG,value+"");
            if(value==null)
                map.put(b,1);
            else
                map.put(b,value+1);
        }
        thresholdCount++;
        ((TextView) findViewById(R.id.debug)).setText(thresholdCount+"");
        
        if (thresholdCount == beaconThreshold) {
            for(ImageView i:beaconImages) i.setVisibility(View.INVISIBLE);

            ArrayList<Beacon> newValidBeacons = getNearBeacons(map, beaconThreshold);
            validBeacons.removeAll(newValidBeacons);
            ((TextView) findViewById(R.id.debug)).setText(validBeacons.toString());
            newParseManager.put(validBeacons, false);
            newParseManager.put(newValidBeacons, true);
            validBeacons = newValidBeacons;

            switch(validBeacons.size()) {
                case 0:
                    status.setText("No beacons detected. Searching...");
                    break;
                case 1:
                    toggleBeaconIcon(validBeacons.get(0), centerImage);
                    status.setText(getColor(validBeacons.get(0))+" beacon detected");
                    break;
                case 2:
                    toggleBeaconIcon(validBeacons.get(0), leftImage);
                    toggleBeaconIcon(validBeacons.get(1), rightImage);
                    status.setText("You're near a " + getColor(validBeacons.get(0)) + " beacon and a " +
                            getColor(validBeacons.get(1)) + " beacon");
                    break;
                case 3:
                    toggleBeaconIcon(validBeacons.get(0), leftImage);
                    toggleBeaconIcon(validBeacons.get(1), rightImage);
                    toggleBeaconIcon(validBeacons.get(2), centerImage);
                    status.setText("You're near a " + getColor(validBeacons.get(0)) + " beacon, a " +
                            getColor(validBeacons.get(1)) + " beacon, and a " + getColor(validBeacons.get(2)) + " beacon");
                    break;
                default:
                    break;
            }
            map.clear();
            thresholdCount = 0;
        }
    }

    private String getColor(Beacon beacon) {
        return newBeaconManager.minorToBeaconAttrib.get(beacon.getMinor()).getColor();
    }

    private ArrayList<Beacon> getNearBeacons(HashMap<Beacon, Integer> map, int beaconThreshold) {
        ArrayList<Beacon> validBeacons = new ArrayList<Beacon>();
        for(Beacon b:map.keySet())
            if(map.get(b)>beaconThreshold/2)
                validBeacons.add(b);
        ((TextView) findViewById(R.id.debug)).setText("Map: " + map.toString());
        return validBeacons;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        newBeaconManager.destroy();
        newParseManager.deleteData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        newBeaconManager.startScanning();
    }

    @Override
    protected void onStop() {
        super.onStop();
        newBeaconManager.stopScanning();
    }

    private boolean hasSameBeacons(List<Beacon> l1, List<Beacon> l2) {
        ArrayList<String> names1 = getBeaconNames(l1);
        ArrayList<String> names2 = getBeaconNames(l2);
        ((TextView) findViewById(R.id.debug)).setText("Incoming Beacons: " + names2.toString() +"\n"+ "Current :"+ names1.toString() +"\n" + thresholdCount);
        for (String n : names2) {
            if (!names1.remove(n)) {
                return false;
            }
        }
        return names1.isEmpty();
    }

    private ArrayList<String> getBeaconNames(List<Beacon> beacons){
        ArrayList<String> names = new ArrayList<String>();
        for (Beacon o : beacons) {
            names.add(o.getMacAddress());
        }
        return names;
    }

    private void toggleBeaconIcon(Beacon b, ImageView imageView){
        imageView.setVisibility(View.VISIBLE);
        //TODO Refactoring Code
        imageView.setImageResource(newBeaconManager.minorToBeaconAttrib.get(b.getMinor()).getImageID());
    }

}
