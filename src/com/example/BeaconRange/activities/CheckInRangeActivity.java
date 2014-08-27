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
import com.example.BeaconRange.BeaconOption;
import com.example.BeaconRange.Beaconizer;
import com.example.BeaconRange.IReceiveBeaconsCallbacks;
import com.example.BeaconRange.R;

import java.util.*;

public class CheckInRangeActivity extends Activity implements IReceiveBeaconsCallbacks {
    Beaconizer newBeaconManager;
    final String TAG = "BEACON";
    final double beaconCutoffDist = 3;
    private int beaconThreshold = 6;
    private int thresholdCount = 0;
    private List<BeaconOption> currentBeaconArray = new ArrayList<BeaconOption>();
    private ArrayList<ImageView> beaconImages = new ArrayList<ImageView>();
    private HashMap<String, Integer> map = new HashMap<String, Integer>();

    private ImageView rightImage, centerImage, leftImage;
    private TextView status;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.check_between_layout);
        Collections.addAll(beaconImages, (ImageView)findViewById(R.id.rightImageView),
                (ImageView) findViewById(R.id.centerImageView),
                (ImageView) findViewById(R.id.leftImageView));
        rightImage = beaconImages.get(0);
        centerImage = beaconImages.get(1);
        leftImage = beaconImages.get(2);
        status = (TextView) findViewById(R.id.status);
        newBeaconManager = new Beaconizer(this, this, beaconCutoffDist);
        Log.d(TAG,"BeaconManager configured.");
    }

    public void onReceiveNearbyBeacons(List<BeaconOption> Beacons) {
        /*
        if(hasSameBeacons(currentBeaconArray, beacons)) {
            thresholdCount += beacons.size();
            if(beacons.size()==0) thresholdCount++;
        }
        else {
            currentBeaconArray = beacons;
            thresholdCount = 0;
        }*/
        for(BeaconOption b:Beacons){
            Integer value = map.get(b.name);
            Log.d(TAG,value+"");
            if(value==null)
                map.put(b.name,1);
            else
                map.put(b.name,value+1);
        }
        thresholdCount++;
        ((TextView) findViewById(R.id.debug)).setText(thresholdCount+"");
        if (thresholdCount == beaconThreshold) {
            for(ImageView i:beaconImages) i.setVisibility(View.INVISIBLE);
            ArrayList<String> validBeacons = getValidBeacons(map, beaconThreshold);
            switch(validBeacons.size()) {
                case 0:
                    status.setText("No Beacons detected. Searching...");
                    break;
                case 1:
                    toggleBeaconIcon(validBeacons.get(0), centerImage);
                    status.setText(validBeacons.get(0) + " detected");
                    break;
                case 2:
                    toggleBeaconIcon(validBeacons.get(0), leftImage);
                    toggleBeaconIcon(validBeacons.get(1), rightImage);
                    status.setText("You're in between the " + validBeacons.get(0) + " beacon and the " +
                            validBeacons.get(1) + " beacon");
                    break;
                case 3:
                    toggleBeaconIcon(validBeacons.get(0), leftImage);
                    toggleBeaconIcon(validBeacons.get(1), rightImage);
                    toggleBeaconIcon(validBeacons.get(2), centerImage);
                    status.setText("You're in the center of all three beacons.");
                    break;
                default:
                    break;
            }
            map.clear();
            thresholdCount = 0;
        }
    }

    private ArrayList<String> getValidBeacons(HashMap<String, Integer> map, int beaconThreshold) {
        ArrayList<String> validNames = new ArrayList<String>();
        for(String name:map.keySet())
            if(map.get(name)>beaconThreshold/2)
                validNames.add(name);
        ((TextView) findViewById(R.id.debug)).setText("Map: " + map.toString());
        return validNames;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        newBeaconManager.destroy();
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

    private boolean hasSameBeacons(List<BeaconOption> l1, List<BeaconOption> l2) {
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

    private ArrayList<String> getBeaconNames(List<BeaconOption> beacons){
        ArrayList<String> names = new ArrayList<String>();
        for (BeaconOption o : beacons) {
            names.add(o.name);
        }
        return names;
    }

    private void toggleBeaconIcon(String name, ImageView imageView){
        imageView.setVisibility(View.VISIBLE);
        //TODO Refactoring Code
        imageView.setImageResource(newBeaconManager.minorToBeaconAttrib.get(name)..getImageID());
    }

}
