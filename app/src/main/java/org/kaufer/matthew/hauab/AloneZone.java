package org.kaufer.matthew.hauab;

/**
 * Created by mjkaufer on 5/17/15.
 */
public class AloneZone {
    private double lon, lat;
    private boolean zone;
    public AloneZone(double lat, double lon, boolean zone){
        this.lon = lon;
        this.lat = lat;
        this.zone = zone;
    }

    public void setZone(boolean newVal){
        this.zone = newVal;
    }

    public boolean isZone(){
        return this.zone;
    }


    public String toString(){
        return lon + ":" + lat + ":" + zone;
    }

    public double getLat(){
        return lat;
    }

    public double getLon(){
        return lon;
    }
}
