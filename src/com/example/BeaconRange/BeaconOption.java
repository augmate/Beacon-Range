package com.example.BeaconRange;

/**
 * Created by darien on 8/18/14.
 */
import com.estimote.sdk.Utils;

import java.io.Serializable;

public class BeaconOption implements Serializable {
    public String name;
    public Utils.Proximity confidence;
    public float distance;

    public BeaconOption(String color, Utils.Proximity confidence, float distance) {
        this.name = color;
        this.confidence = confidence;
        this.distance = distance;
    }
}
