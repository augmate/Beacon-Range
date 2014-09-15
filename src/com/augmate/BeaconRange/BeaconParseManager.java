package com.augmate.BeaconRange;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;
import com.parse.*;
import android.provider.Settings.Secure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Darien on 8/28/14.
 * Manager for handling Parse calls regarding newly discovered beacons
 */
public class BeaconParseManager {
    final String YOUR_APPLICATION_ID = "kSOqeIVQCitrSI2OEbUnpXVmbVxzmuPK610CzCZA";
    final String YOUR_CLIENT_KEY = "3Ekrf6Ak793ShNkeyAPFdI3UnQnNpExzjmZFzJUZ";
    final private String BEACON_QUERY = "Beacon";
    final private String PARSE_INFO = "ParseBeacon";
    private TextView status;
    TextView viewById;
    private Beaconizer beaconizer;
    private ParseUser User;
    private Activity main;
    private HashMap<Beacon, ParseObject> beaconToParseObj = new HashMap<Beacon, ParseObject>();


    public BeaconParseManager(Activity main, Beaconizer beaconizer) {
        this.beaconizer = beaconizer;
        this.main = main;
        viewById = (TextView) main.findViewById(R.id.debug);
        status = (TextView) main.findViewById(R.id.status);
        Parse.initialize(main, YOUR_APPLICATION_ID, YOUR_CLIENT_KEY);
        User = new ParseUser();
        String username = Secure.getString(main.getContentResolver(),Secure.ANDROID_ID);
        String password = "augmate";
        viewById.setText("ATTEMPTING SIGN UP/SIGN IN");
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
                    status.setText("Connected!");
                    beaconizer.startScanning();
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
                    status.setText("Connected!");
                    beaconizer.startScanning();
                } else {
                    Log.d(PARSE_INFO, "Error: " + e.getMessage());
                    viewById.setText("SIGN FAILED. PLEASE CLOSE THE APPLICATION AND TRY AGAIN.");
                }
            }
        });
    }

    public void put(ArrayList<Beacon> discoveredBeacons, ArrayList<Beacon> validBeacons){
        if(User.isAuthenticated()){
            if(!discoveredBeacons.isEmpty())
                BeaconUpdate(discoveredBeacons);
            UserUpdate(validBeacons);
        }
    }

    private void UserUpdate(final ArrayList<Beacon> validBeacons) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(BEACON_QUERY);
        query.whereContainedIn("macAddress", getMacAddresses(validBeacons));
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> parseBeacons, ParseException e) {
                if (e == null) {
                    User.put("userBeaconArray", parseBeacons);
                    if (!validBeacons.isEmpty())
                        for(ParseObject pB : parseBeacons){
                            if(pB.getString("macAddress").equals(validBeacons.get(0).getMacAddress())){
                                User.put("lastSeenAt", pB);
                                break;
                            }
                        }
                    saveParseObject(User);
                } else {
                    Log.d(PARSE_INFO, "Error: " + e.getMessage());
                }
            }
        });
    }

    /*
    An update method based on using Parse Relations instead of Parse arrays. May implement later since it is a more
    accurate representation of the parse database.
    private void UserUpdate(final ArrayList<Beacon> validBeacons) {
        User.getRelation("myBeacons").getQuery().findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> parseBeacons, ParseException e) {
                if (e == null) {
                    try {
                        ParseRelation<ParseObject> relation = User.getRelation("myBeacons");
                        for (ParseObject r : parseBeacons) relation.remove(r);
                        saveParseObject(User);
                        ParseQuery<ParseObject> query = ParseQuery.getQuery(BEACON_QUERY);
                        query.whereContainedIn("macAddress", getMacAddresses(validBeacons));
                        query.findInBackground(new FindCallback<ParseObject>() {
                            public void done(List<ParseObject> parseBeacons, ParseException e) {
                                if (e == null) {
                                    ParseRelation<ParseObject> relation = User.getRelation("myBeacons");
                                    for (ParseObject r : parseBeacons) relation.add(r);
                                    if (!validBeacons.isEmpty())
                                        for(ParseObject pB : parseBeacons){
                                            if(pB.getString("macAddress").equals(validBeacons.get(0).getMacAddress())){
                                                User.put("lastSeenAt", pB);
                                                break;
                                            }
                                        }
                                    saveParseObject(User);
                                } else {
                                    Log.d(PARSE_INFO, "Error: " + e.getMessage());
                                }
                            }
                        });
                    } catch (NullPointerException x) {
                        Log.d("PARSE", "Error1: " + x.getMessage());
                    }
                } else {
                    Log.d("PARSE", "Error2: " + e.getMessage());
                    User.saveInBackground();
                }
            }
        });
    }
    */

    private void BeaconUpdate(final ArrayList<Beacon> discoveredBeacons) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(BEACON_QUERY);
        Log.d(PARSE_INFO, "Starting Beacon Query");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> parseBeacons, ParseException e) {
                //Log.d(PARSE_INFO, "Finished Beacon Query. " + parseBeacons.size() + " beacons are in Parse");
                ArrayList<String> parseMacs = getMacAddresses(parseBeacons);
                for(Beacon b : discoveredBeacons){
                    for(ParseObject pB : parseBeacons){
                        Log.d(PARSE_INFO,b.getMacAddress().equals(pB.getString("macAddress")) + ": "+b.getMacAddress()+"V.S."+pB.getString("macAddress"));
                        if(b.getMacAddress().equals(pB.getString("macAddress"))){ //within this loop, update any beacons that are already known by parse
                            beaconToParseObj.put(b, pB);
                            setParseParams(b, pB);
                            break;
                        }
                    }
                    if(!parseMacs.contains(b.getMacAddress())){ //if that Beacon is not known by Parse, add a brand new beacon to Parse
                        ParseObject parseBeacon = new ParseObject(BEACON_QUERY);
                        beaconToParseObj.put(b, parseBeacon);
                        setParseParams(b, parseBeacon);
                    }
                }
            }
        });
    }

    private ArrayList<String> getMacAddresses(List<ParseObject> parseBeacons) {
        ArrayList<String> macAddresses = new ArrayList<String>();
        try{ // for some reason this for loop causes a null pointer exception sometimes. Not sure how to fix.
            for(ParseObject b: parseBeacons)
                macAddresses.add(b.getString("macAddress"));
            return macAddresses;
        }
        catch (NullPointerException e){
            return null;
        }
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
        parseBeacon.put("lastUpdatedBy", User);
        if(parseBeacon.getObjectId() == null) parseBeacon.put("discoveredBy", User);
        Log.d(PARSE_INFO, "Attempting to save " + parseBeacon.getObjectId());
        saveParseObject(parseBeacon);
    }

    private void saveParseObject(final ParseObject parseObj) {
        parseObj.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(PARSE_INFO, "Saved " + parseObj.getObjectId());
                } else {
                    Log.d(PARSE_INFO, "Failed to Save " + parseObj.getObjectId());
                    e.printStackTrace();
                }
            }
        });
    }

    public void findUser(String s) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("firstName", s);
        query.include("lastSeenAt");
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> objects, ParseException e) {
                if (e == null && objects.size()>0) {
                    ParseUser u = objects.get(0);
                    Toast.makeText(main,u.get("firstName") + " was last seen at Beacon " +
                            u.getParseObject("lastSeenAt").get("minor") + " around " + u.getUpdatedAt() ,  Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(main,"User not found!" ,  Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
