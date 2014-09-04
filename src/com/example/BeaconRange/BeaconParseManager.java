package com.example.BeaconRange;

import android.app.Activity;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;
import com.parse.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Darien on 8/28/14.
 */
public class BeaconParseManager {
    private Beaconizer beaconizer;
    private HashMap<Beacon, ParseObject> beaconToParseObj = new HashMap<Beacon, ParseObject>();
    final private String QUERY_NAME = "Beacon";
    final private String BEACONS_LABEL = "updatedBeacons"
    final String YOUR_APPLICATION_ID = "JdeDnORM2uskVmy91dcJZiWnY8ITPZcBrg2RRNht";
    final String YOUR_CLIENT_KEY = "f8VQFqtfDIh8rvMzMaclTsDoiH6cg9Rf5Tbz2jcZ";

    public BeaconParseManager(Activity main, Beaconizer beaconizer) {
        this.beaconizer = beaconizer;
        Parse.enableLocalDatastore(main);
        Parse.initialize(main, YOUR_APPLICATION_ID, YOUR_CLIENT_KEY);
        ParseUser.enableAutomaticUser();
        ParseUser.getCurrentUser().increment("RunCount");
        ParseUser.getCurrentUser().saveInBackground();
        //ParseACL defaultACL = new ParseACL();
        //defaultACL.setPublicReadAccess(true);
        //ParseACL.setDefaultACL(defaultACL, true);
        syncToNetwork();

    }



    public void put(ArrayList<Beacon> beacons, final boolean state){
        //find all the macAddresses of all beacon currently known of by this user. These adddress would be the local parse database
        final ArrayList<String> macAddresses = getMacAddresses(beacons);
        ParseQuery<ParseObject> query = ParseQuery.getQuery(QUERY_NAME);
        query.fromLocalDatastore();
        query.whereContainedIn("macAddress", macAddresses);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> parseBeacons, ParseException e) {
                ArrayList<String> macAddressesCopy = macAddresses;
                if (e == null) {
                    for (ParseObject b : parseBeacons)
                        macAddressesCopy.remove(b.getString("macAddress"));
                } else {
                    // There was an error.
                }
                //If there are any macaddress that were not found in the local database,the associated beacons must be added to the network database
                if(!macAddressesCopy.isEmpty()){
                    for(String mac : macAddresses)
                        parsePut(true, beaconizer.getBeaconFromMacAddress(mac));
                    syncToNetwork();
                }
            }
        });

        for(final Beacon b : beacons){ //Perform parse update if this beacon has been seen before. Else add this beacon to the parse database
            if(beaconToParseObj.containsKey(b)){
                parseUpdate(state, b);
            }
            else{
                parsePut(state, b);
            }
        }
    }

    private void parsePut(boolean state, Beacon b) {
        ParseObject parseBeacon = new ParseObject(QUERY_NAME);
        setParseParams(b, state, parseBeacon);
        beaconToParseObj.put(b, parseBeacon);
    }

    private void parseUpdate(final boolean state, final Beacon b) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(QUERY_NAME);
        query.getInBackground(beaconToParseObj.get(b).getObjectId(), new GetCallback<ParseObject>() {
            public void done(ParseObject parseBeacon, ParseException e) {
                if (e == null) {
                    setParseParams(b, state, parseBeacon);
                }
            }
        });
    }

    private void syncToNetwork() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(QUERY_NAME);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(final List<ParseObject> parseBeacons, ParseException e) {
                if (e == null) {
                    return;
                }
                // Release any objects previously pinned for this query.
                ParseObject.unpinAllInBackground(BEACONS_LABEL, parseBeacons, new DeleteCallback() {
                    public void done(ParseException e) {
                        if (e != null) {
                            // There was some error.
                            return;
                        }
                        // Add the latest results for this query to the cache.
                        ParseObject.pinAllInBackground(BEACONS_LABEL, parseBeacons);
                    }
                });
            }
        });
    }

    private ArrayList<String> getMacAddresses(ArrayList<Beacon> beacons) {
        ArrayList<String> macAddresses = new ArrayList<String>();
        for(Beacon b:beacons)
            macAddresses.add(b.getMacAddress());
        return macAddresses;
    }

    private void setParseParams(Beacon b, boolean state, ParseObject parseBeacon) {
        parseBeacon.put("color", beaconizer.getBeaconColor(b));
        parseBeacon.put("isOn", state);
        parseBeacon.put("macAddress",b.getMacAddress());
        parseBeacon.put("major",b.getMajor());
        parseBeacon.put("minor",b.getMinor());
        parseBeacon.put("rssi",b.getRssi());
        parseBeacon.put("distance",(float) Utils.computeAccuracy(b));
        parseBeacon.put("proximity", Utils.computeProximity(b).toString());
        //parseBeacon.put("measuredPower",b.getMeasuredPower());
        parseBeacon.put("UUID",b.getProximityUUID());
        parseBeacon.put("user", ParseUser.getCurrentUser());
        parseBeacon.pinInBackground();
    }

    public void deleteData(){
        try {
            ParseObject.deleteAll(new ArrayList(beaconToParseObj.values()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        /*
        for(ParseObject beacon : beaconToParseObj.values()){
            beacon.deleteEventually();
        }
        */
    }
}
