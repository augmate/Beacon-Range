package com.example.BeaconRange;

import android.app.Activity;
import android.util.Log;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;
import com.parse.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Darien on 8/28/14.
 */
public class BeaconParseManager {
    private Beaconizer beaconizer;
    private ParseUser User;
    private HashMap<Beacon, ParseObject> beaconToParseObj = new HashMap<Beacon, ParseObject>();
    final private String BEACON_QUERY = "Beacon";
    final private String PARSE_INFO = "ParseBeacon";
    final String YOUR_APPLICATION_ID = "kSOqeIVQCitrSI2OEbUnpXVmbVxzmuPK610CzCZA";
    final String YOUR_CLIENT_KEY = "3Ekrf6Ak793ShNkeyAPFdI3UnQnNpExzjmZFzJUZ";

    public BeaconParseManager(Activity main, Beaconizer beaconizer) {
        this.beaconizer = beaconizer;
        Parse.initialize(main, YOUR_APPLICATION_ID, YOUR_CLIENT_KEY);
        User = new ParseUser();
        String username = android.os.Build.MODEL + android.os.Build.ID;
        String password = "augmate";
        ParseSignUp(username, password);
    }

    private void ParseSignUp(final String username, final String password) {
        User.setUsername(username);
        User.setPassword(password);
        Log.d(PARSE_INFO, "ATTEMPTING SIGN UP");
        User.signUpInBackground(new SignUpCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(PARSE_INFO, "SIGNED UP!");
                    User.increment("RunCount");
                } else {
                    Log.d(PARSE_INFO, "SIGN UP FAILED, ATTEMPTING LOGIN");
                    ParseLogin(username, password);
                }
            }
        });
    }

    private void ParseLogin(final String username, final String password) {
        Log.d(PARSE_INFO, "LOGGING IN");


        User.logInInBackground(username, password, new LogInCallback() {
            public void done(ParseUser user, ParseException e) {
                if (user != null) {
                    Log.d(PARSE_INFO, "LOGGED IN!");
                    User = user;
                    User.increment("RunCount");
                    //saveParseObject(User);
                } else {

                }
            }
        });
    }


    public void put(ArrayList<Beacon> removedBeacons, ArrayList<Beacon> discoveredBeacons, ArrayList<Beacon> consistentBeacons, ArrayList<Beacon> validBeacons){
        if(User.isAuthenticated()){
            if(!discoveredBeacons.isEmpty())
                BeaconUpdate(discoveredBeacons);
            UserUpdate(validBeacons);
        }
    }

    private void UserUpdate(ArrayList<Beacon> validBeacons) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(BEACON_QUERY);
        query.whereContainedIn("macAddress", getMacAddresses(validBeacons));
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> parseBeacons, ParseException e) {
                if (e == null) {
                    User.put("userBeaconArray", parseBeacons);
                    saveParseObject(User);
                } else {
                    Log.d("score", "Error: " + e.getMessage());
                }
            }
        });
    }
    /* RELATION UPDATE. FREEZES APPLICATION
    private void UserUpdate(final ArrayList<Beacon> validBeacons) {
        final ParseRelation<ParseObject> relation = User.getRelation("myBeacons");
        ParseQuery<ParseObject> query = relation.getQuery();
        List<ParseObject> parseBeacons = new ArrayList<ParseObject>();
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> parseBeacons, ParseException e) {
                if (e == null) {
                    try {
                        ParseRelation<ParseObject> relation = User.getRelation("myBeacons");
                        for (ParseObject r : parseBeacons) relation.remove(r);
                        User.saveInBackground();
                        for (Beacon v : validBeacons) relation.add(beaconToParseObj.get(v));
                        User.saveInBackground();
                    } catch (NullPointerException x) {
                        Log.d("PARSE", "Error: " + x.getMessage());
                    }
                } else {
                    Log.d("PARSE", "Error: " + e.getMessage());
                    User.saveInBackground();
                }
            }
        });
    }
    */

    private void BeaconUpdate(final ArrayList<Beacon> discoveredBeacons) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(BEACON_QUERY);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> parseBeacons, ParseException e) {
                ArrayList<String> parseMacs = getMacAddresses(parseBeacons);
                for(Beacon b : discoveredBeacons){
                    for(ParseObject pB : parseBeacons){
                        if(b.getMacAddress().equals(pB.getString("macAddress"))){ //within this loop, update any beacons that are already known by parse
                            beaconToParseObj.put(b, pB);
                            setParseParams(b, beaconToParseObj.get(b));
                            break;
                        }
                    }
                    if(!parseMacs.contains(b.getMacAddress())){ //if that Beacon is not known by Parse, add a brand new beacon to Parse
                        ParseObject parseBeacon = new ParseObject(BEACON_QUERY);
                        setParseParams(b, parseBeacon);
                        beaconToParseObj.put(b, parseBeacon);
                    }
                }
            }
        });
    }

    private ArrayList<String> getMacAddresses(List<ParseObject> parseBeacons) {
        ArrayList<String> macAddresses = new ArrayList<String>();
        for(ParseObject b: parseBeacons)
            macAddresses.add(b.getString("macAddress"));
        return macAddresses;
    }


    private ArrayList<String> getMacAddresses(ArrayList<Beacon> beacons) {
        ArrayList<String> macAddresses = new ArrayList<String>();
        for(Beacon b:beacons)
            macAddresses.add(b.getMacAddress());
        return macAddresses;
    }

    private void setParseParams(Beacon b, ParseObject parseBeacon) {
        parseBeacon.put("color", beaconizer.getBeaconColor(b));
        parseBeacon.put("macAddress",b.getMacAddress());
        parseBeacon.put("major",b.getMajor());
        parseBeacon.put("minor",b.getMinor());
        parseBeacon.put("rssi",b.getRssi());
        parseBeacon.put("distance",(float) Utils.computeAccuracy(b));
        parseBeacon.put("proximity", Utils.computeProximity(b).toString());
        //parseBeacon.put("measuredPower",b.getMeasuredPower());
        parseBeacon.put("UUID",b.getProximityUUID());
        parseBeacon.put("lastUpdatedBy", ParseUser.getCurrentUser());
        saveParseObject(parseBeacon);

    }

    private void saveParseObject(final ParseObject parseObj) {
        parseObj.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(PARSE_INFO, "Saved "+ parseObj.getObjectId());
                } else {
                    Log.d(PARSE_INFO, "Failed to Save "+ parseObj.getObjectId());
                    e.printStackTrace();
                }
            }
        });
    }

    public void deleteData(){
        try {
            ParseObject.deleteAll(new ArrayList(beaconToParseObj.values()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
