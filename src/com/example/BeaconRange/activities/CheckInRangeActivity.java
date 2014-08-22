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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CheckInRangeActivity extends Activity implements IReceiveBeaconsCallbacks {
    Beaconizer newBeaconManager;
    final String TAG = "BEACON";
    final double beaconCutoffDist = 3;
    private int beaconThreshold = 6;
    private int thresholdCount = 0;
    private List<BeaconOption> currentBeaconArray = new ArrayList<BeaconOption>();
    private ArrayList<ImageView> beaconImages = new ArrayList<ImageView>();
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

    public void onReceiveNearbyBeacons(List<BeaconOption> beacons) {
        if(hasSameBeacons(currentBeaconArray, beacons)) {
            thresholdCount += beacons.size();
            if(beacons.size()==0) thresholdCount++;
        }
        else {
            currentBeaconArray = beacons;
            thresholdCount = 0;
        }

        if (thresholdCount == beaconThreshold) {
            for(ImageView i:beaconImages) i.setVisibility(View.INVISIBLE);
            switch(beacons.size()) {
                case 0:
                    status.setText("No Beacons detected. Searching...");
                    break;
                case 1:
                    toggleBeaconIcon(beacons.get(0).name, centerImage);
                    status.setText(beacons.get(0).name + " detected");
                    break;
                case 2:
                    toggleBeaconIcon(beacons.get(0).name, leftImage);
                    toggleBeaconIcon(beacons.get(1).name, rightImage);
                    status.setText("You're in between the " + beacons.get(0).name + " beacon and the " +
                            beacons.get(1).name + " beacon");
                    break;
                case 3:
                    toggleBeaconIcon(beacons.get(0).name, leftImage);
                    toggleBeaconIcon(beacons.get(1).name, rightImage);
                    toggleBeaconIcon(beacons.get(2).name, centerImage);
                    status.setText("You're in the center of all three beacons.");
                    break;
                default:
                    break;
            }
            thresholdCount = 0;
        }
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
        imageView.setImageResource(newBeaconManager.getNameToImageID().get(name));
    }

}
