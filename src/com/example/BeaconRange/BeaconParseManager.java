package com.example.BeaconRange;

import com.estimote.sdk.Beacon;
import com.parse.ParseObject;

import java.util.HashMap;

/**
 * Created by Darien on 8/28/14.
 */
public class BeaconParseManager {
    private HashMap<Integer, BeaconAttrib> minorToBeaconAttrib;

    public BeaconParseManager(HashMap<Integer, BeaconAttrib> map) {
        this.minorToBeaconAttrib = map;
    }
    public void put(Beacon b){
        ParseObject gameScore = new ParseObject("Beacon");
        gameScore.put("color", minorToBeaconAttrib.get());
        gameScore.put("playerName", "Sean Plott");
        gameScore.put("cheatMode", false);
        gameScore.saveInBackground();

    }
}
