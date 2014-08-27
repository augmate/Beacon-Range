package com.example.BeaconRange;

/**
 * Created by Darien on 8/27/14.
 */
public class BeaconAttrib{


    private String color;
    private int ImageID;

    public BeaconAttrib(String color, int ID) {
        this.color = color;
        this.ImageID = ID;
    }

    public String getColor() {
        return color;
    }
    public int getImageID() {
        return ImageID;
    }
}
