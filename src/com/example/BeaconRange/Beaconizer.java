package com.example.BeaconRange;

/**
 * Created by darien on 8/18/14.
 */

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;

import java.util.*;

public class Beaconizer {

    private static final Region BEACON_SEARCH_MASK = new Region("rid", null, null, null);
    final String TAG = "BEACONIZER";
    private BeaconManager beaconManager;
    boolean isRunning = false;

    private HashMap<String, Integer> nameToImageID = new HashMap<String, Integer>();

    public Beaconizer(Context context, final IReceiveBeaconsCallbacks receiver, final double beaconCutoffDist) {
        Log.d(TAG,"Beacon Service is ready. Starting ranging scan.");
        nameToImageID.put("purple", R.drawable.purple_beacon);
        nameToImageID.put("blue", R.drawable.blue_beacon);
        nameToImageID.put("green", R.drawable.green_beacon);
        beaconManager = new BeaconManager(context);

        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override public void onBeaconsDiscovered(Region region, List<Beacon> rawBeacons) {
                Log.d(TAG,"BLE-scan found ranged beacons: " + rawBeacons);

                if(!isRunning) {
                    Log.d(TAG,"got results after stopping beacon manager");
                    return;
                }

                // cut-off point for beacons
                // when at 15% broadcast power, 4 seems to be far enough to ignore
                // TODO: does the reported beacon distance change with power?
                ///double beaconCutoffDist = 2;

                List<BeaconData> processedBeacons = new ArrayList<BeaconData>();

                for(Beacon beacon : rawBeacons) {
                    BeaconData processedBeacon = getBeaconData(beacon);

                    if(processedBeacon.distance < beaconCutoffDist && processedBeacon.signalStrength >-90) {
                    //if(processedBeacon.proximity != Utils.Proximity.FAR){
                        processedBeacons.add(processedBeacon);
                    }
                }

                Collections.sort(processedBeacons, new Comparator<BeaconData>() {
                    @Override
                    public int compare(BeaconData lhs, BeaconData rhs) {
                        // assuming precision down to 0.01 units ("meters")
                        return (int) (100 * (lhs.distance - rhs.distance));
                    }
                });


                List<BeaconOption> beacons = new ArrayList<BeaconOption>();
                for (BeaconData processedBeacon : processedBeacons) {
                    for(String color : processedBeacon.colors)
                        beacons.add(new BeaconOption(color, processedBeacon.proximity, processedBeacon.distance));
                }

                receiver.onReceiveNearbyBeacons(beacons);
            }
        });
    }

    public void destroy() {
        Log.d(TAG,"Destroying manager..");
        isRunning = false;
        beaconManager.disconnect();
    }

    public void startScanning() {
        // TODO: handle bluetooth errors
        Log.d(TAG, "Starting beacon manager..");
        assert(isRunning);
        isRunning = true;

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override public void onServiceReady() {
                Log.d(TAG, "Beacon Service is ready. Starting ranging scan.");

                try {
                    beaconManager.startRanging(BEACON_SEARCH_MASK);
                } catch (RemoteException e) {
                    Log.e(TAG, "BeaconManager couldn't start ranging.", e);
                }
            }
        });
    }

    public void stopScanning() {
        Log.d(TAG, "Stopping scanner");

        if(!isRunning)
            return;

        isRunning = false;
        try {
            beaconManager.stopRanging(BEACON_SEARCH_MASK);
        } catch (RemoteException e) {
            Log.e(TAG, "Can't stop Beacon Manager", e);
        }
    }

    private class BeaconData {

        public List<String> colors = new ArrayList<String>(); // colors/resources this beacon represents
        public Utils.Proximity proximity; // proximity of distance
        public float distance; // distance to user
        public int signalStrength; //0 is strongest and -100 is weakest
    }
    /*
        Beacon proximity
        -------------------------------------------------
        Computes Utils.Proximity based on distance in meters. Current distance ranges:
        immediate: 0 - 0.5m
        near: 0.5m - 3.0m
        far: 3.0m - ...

        beacon identities
        -------------------------------------------------
        color           major:minor
        purple          40125:2233
        blue      1:9
        green     1:7

        once we can modify beacon data, this won't be necessary
     */
    protected String getBeaconName(Beacon beacon) {
        String name = beacon.getMajor() + ":" + beacon.getMinor();

        if(name.equals("40125:2233"))
            return "purple";
        if(name.equals("1:9"))
            return "blue";
        if(name.equals("1:7"))
            return "green";

        return "(unknown beacon)";
    }

    protected BeaconData getBeaconData(Beacon beacon) {
        String beaconName = getBeaconName(beacon);
        float distance = (float) Utils.computeAccuracy(beacon);
        int rssi = beacon.getRssi(); //Received Signal Strength Indication
        Utils.Proximity proximity = Utils.computeProximity(beacon);

        Log.d(TAG, "  Beacon " + beaconName + " accuracy=" + String.format("%.2f", distance) + " power=" + beacon.getMeasuredPower() +
                " rssi=" + beacon.getRssi() + " proximity" + proximity);

        // beacon data could be stored in the beacon, or pulled from a web-service
        BeaconData data = new BeaconData();
        data.distance = distance;
        data.proximity = proximity;
        data.signalStrength = rssi;

        if (beaconName.equals("purple")) {
            data.colors.add("purple");
        } else if (beaconName.equals("blue")) {
            data.colors.add("blue");
        } else if (beaconName.equals("green")) {
            data.colors.add("green");
        }
        return data;
    }

    public HashMap<String, Integer> getNameToImageID() {
        return nameToImageID;
    }
}
