package se.torsteneriksson.timetogo;

import android.location.Location;

/**
 * Created by torsten on 11/11/2016.
 */

public class Position {
    private String mAddress;
    private Location mLocation;

    public Location getLocation() {
        return mLocation;
    }

    public void setLocation(Location mLocation) {
        this.mLocation = mLocation;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String mAddress) {
        this.mAddress = mAddress;
    }
}
