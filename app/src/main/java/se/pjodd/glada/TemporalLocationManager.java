package se.pjodd.glada;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * @author kalle
 * @since 2017-05-13 06:32
 */

public class TemporalLocationManager {

    private Context context;
    private LocationManager locationManager;

    public TemporalLocationManager(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * https://en.wikipedia.org/wiki/Dilution_of_precision_(navigation)
     */
    private float maximumPdop = 4f;

    /**
     * In meters
     */
    private float maximumAccuracy = 10f;

    private long minimumMillisecondsBetweenUpdates = TimeUnit.SECONDS.toMillis(1);

    /**
     * For how long we remember the locations
     */
    private long minimumMillisecondsMemory = TimeUnit.MINUTES.toMillis(1);

    private final LinkedList<Position> memory = new LinkedList<>();


    private void pruneMemory() {
        synchronized (memory) {
            for (Iterator<Position> iterator = memory.iterator(); iterator.hasNext(); ) {
                Position position = iterator.next();
                if (position.getTimestamp() > System.currentTimeMillis() + minimumMillisecondsMemory) {
                    iterator.remove();
                } else {
                    break;
                }
            }
        }
    }

    private Comparator<Position> timestampPositionComparator = new Comparator<Position>() {
        @Override
        public int compare(Position lhs, Position rhs) {
            return lhs.getTimestamp() < rhs.getTimestamp() ? -1 : (lhs.getTimestamp() == rhs.getTimestamp() ? 0 : 1);
        }
    };

    public Position getLocation(long timestamp) {
        synchronized (memory) {
            if (memory.isEmpty()) {
                return null;
            }
            int index = Collections.binarySearch(memory, new Position(timestamp, -1d, -1d, -1f, -1d), timestampPositionComparator);
            if (index < 0) {
                index *= -1;
            }
            Position p1 = memory.get(index);
            if (timestamp == p1.getTimestamp()) {
                return p1;
            } else {
                Position p2 = null;
                if (timestamp < p1.getTimestamp() && index > 0) {
                    p2 = p1;
                    p1 = memory.get(index - 1);

                } else if (timestamp > p1.getTimestamp() && index < memory.size() - 1) {
                    p2 = memory.get(index + 1);
                }
                if (p2 == null) {
                    return p1;
                } else {
                    // todo weight distance and time to produce a new position
                    return memory.get(index);
                }
            }
        }
    }

    public boolean start() {

        pruneMemory();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        locationManager.addNmeaListener(nmeaListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minimumMillisecondsBetweenUpdates, 0, locationListener);

        return true;
    }

    public void stop() {
        locationManager.removeNmeaListener(nmeaListener);
        locationManager.removeUpdates(locationListener);
    }

    private NmeaParserListener nmeaListener = new NmeaParserListener();

    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            if (nmeaListener.getPdop() != null && maximumPdop <= nmeaListener.getPdop()
                    && maximumAccuracy <= location.getAccuracy()) {

                synchronized (memory) {
                    memory.add(new Position(
                            location.getTime(),
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getAccuracy(),
                            nmeaListener.getPdop()
                    ));
                }
            }

            pruneMemory();
        }


        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };


    public static class Position {
        private long timestamp;
        private double latitude;
        private double longitude;
        private float accuracy;
        private double pdop;

        public Position(long timestamp, double latitude, double longitude, float accuracy, double pdop) {
            this.timestamp = timestamp;
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
            this.pdop = pdop;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public float getAccuracy() {
            return accuracy;
        }

        public double getPdop() {
            return pdop;
        }
    }

}
