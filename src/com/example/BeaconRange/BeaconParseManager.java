package com.example.BeaconRange;

import android.app.Activity;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;
import com.example.BeaconRange.activities.CheckInRangeActivity;
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
    private String QUERY_NAME = "Beacon";
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

    }
    public void put(ArrayList<Beacon> beacons, final boolean state){
        for(final Beacon b : beacons){ //Perform parse update if this beacon has been seen before. Else add this beacon to the parse database
            if(beaconToParseObj.containsKey(b)){
                ParseQuery<ParseObject> query = ParseQuery.getQuery(QUERY_NAME);
                query.getInBackground(beaconToParseObj.get(b).getObjectId(), new GetCallback<ParseObject>() {
                    public void done(ParseObject parseBeacon, ParseException e) {
                        if (e == null) {
                            setParseParams(b, state, parseBeacon);
                        }
                    }
                });
            }
            else{
                ParseObject parseBeacon = new ParseObject(QUERY_NAME);
                setParseParams(b, state, parseBeacon);
                beaconToParseObj.put(b, parseBeacon);
            }
        }
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
        if(ParseUser.getCurrentUser()!=null)
            parseBeacon.put("user", ParseUser.getCurrentUser());
        else
            parseBeacon.put("user", "x");
        parseBeacon.saveInBackground();
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
