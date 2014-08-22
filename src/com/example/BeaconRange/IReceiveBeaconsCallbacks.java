package com.example.BeaconRange;

import java.util.List;

public interface IReceiveBeaconsCallbacks {
    void onReceiveNearbyBeacons(List<BeaconOption> beacons);
}
