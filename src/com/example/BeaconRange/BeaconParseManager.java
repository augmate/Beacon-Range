package com.example.BeaconRange;

import com.estimote.sdk.Beacon;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Darien on 8/28/14.
 */
public class BeaconParseManager {
    private DefaultHashMap<Integer, BeaconAttrib> minorToBeaconAttrib;
    private HashMap<Beacon, ParseObject> beaconToParseObj = new HashMap<Beacon, ParseObject>();
    private String QUERY_NAME = "TestObject";

    public BeaconParseManager(DefaultHashMap<Integer, BeaconAttrib> map) {
        this.minorToBeaconAttrib = map;
    }
    public void put(ArrayList<Beacon> beacons, boolean state){
        for(Beacon b : beacons){
            if(beaconToParseObj.containsKey(b))
                parseUpdate(b, state);
            else
                parsePut(b, state);

        }
    }

    private void parseUpdate(final Beacon b, final boolean state) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(QUERY_NAME);
        // Retrieve the object by id
        query.getInBackground(beaconToParseObj.get(b).getObjectId(), new GetCallback<ParseObject>() {
            public void done(ParseObject parseBeacon, ParseException e) {
                if (e == null) {
                    parseBeacon.put("color", minorToBeaconAttrib.get(b.getMinor()).getColor());
                    parseBeacon.put("isOn", state);
                    parseBeacon.saveInBackground();
                }
            }
        });
    }

    private void parsePut(Beacon b, boolean state) {
        ParseObject parseBeacon = new ParseObject(QUERY_NAME);
        parseBeacon.put("color", minorToBeaconAttrib.get(b.getMinor()).getColor());
        parseBeacon.put("isOn", state);
        parseBeacon.saveInBackground();
        beaconToParseObj.put(b, parseBeacon);
    }

    public void deleteData(){
        try {
            ParseObject.deleteAll(new ArrayList(beaconToParseObj.values()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
