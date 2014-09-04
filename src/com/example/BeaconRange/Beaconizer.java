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
    private HashMap<String, Beacon> macToBeacon= new HashMap<String, Beacon>();
    private static final DefaultHashMap<Integer, BeaconAttrib> minorToBeaconAttrib;
    static
    {
        minorToBeaconAttrib = new DefaultHashMap<Integer, BeaconAttrib>(new BeaconAttrib("unknown", R.drawable.grey_beacon));
        minorToBeaconAttrib.put(2233, new BeaconAttrib("purple", R.drawable.purple_beacon));
        minorToBeaconAttrib.put(9, new BeaconAttrib("blue", R.drawable.blue_beacon));
        minorToBeaconAttrib.put(7, new BeaconAttrib("green", R.drawable.green_beacon));
    }


    public Beaconizer(Context context, final IReceiveBeaconsCallbacks receiver, final double beaconCutoffDist) {
        Log.d(TAG,"Beacon Service is ready. Starting ranging scan.");
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

                List<Beacon> processedBeacons = new ArrayList<Beacon>();

                for(Beacon beacon : rawBeacons) {
                    String color = minorToBeaconAttrib.get(beacon.getMinor()).getColor();
                    if(color ==null) color = "unknown";
                    float distance = (float) Utils.computeAccuracy(beacon);
                    int rssi = beacon.getRssi(); //Received Signal Strength Indication
                    Utils.Proximity proximity = Utils.computeProximity(beacon);


                    if(distance < beaconCutoffDist) {
                    //if(processedBeacon.proximity != Utils.Proximity.FAR){
                        processedBeacons.add(beacon);
                        macToBeacon.put(beacon.getMacAddress(), beacon);
                    }

                    Log.d(TAG, "  Beacon " + color + " accuracy=" + String.format("%.2f", distance) + " power=" + beacon.getMeasuredPower() +
                            " rssi=" + rssi + " proximity" + proximity);
                }

                Collections.sort(processedBeacons, new Comparator<Beacon>() {
                    @Override
                    public int compare(Beacon lhs, Beacon rhs) {
                        // assuming precision down to 0.01 units ("meters")
                        return (int) (100 * ((float) Utils.computeAccuracy(lhs) - (float) Utils.computeAccuracy(rhs)));
                    }
                });
                receiver.onReceiveNearbyBeacons(processedBeacons);
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

    public String getBeaconColor(Beacon b){
        return minorToBeaconAttrib.get(b.getMinor()).getColor();
    }

    public int getBeaconImage(Beacon b){
        return minorToBeaconAttrib.get(b.getMinor()).getImageID();
    }

    public Beacon getBeaconFromMacAddress(String mac){
        return macToBeacon.get(mac);
    }
}
