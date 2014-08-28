package com.example.BeaconRange;

import com.estimote.sdk.Beacon;

import java.util.List;

public interface IReceiveBeaconsCallbacks {
    void onReceiveNearbyBeacons(List<Beacon> beacons);
}
