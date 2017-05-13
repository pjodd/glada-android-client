package se.pjodd.glada;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import se.pjodd.glada.db.DatabaseContract;
import se.pjodd.glada.db.DatabaseHelper;

public class TrackerService extends Service {

    private double maximumPdop = 2d;
    private float maximumAccuracy = 20f;

    public TrackerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private DatabaseHelper databaseHelper;
    private SQLiteDatabase db;

    private WifiManager wifiManager;

    private LocationManager locationManager;



    private NmeaParserListener nmeaListener = new NmeaParserListener();

    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            if (nmeaListener.getPdop()!= null && maximumPdop <= nmeaListener.getPdop()
                    && maximumAccuracy <= location.getAccuracy()) {

                ContentValues values = new ContentValues();
                values.put(DatabaseContract.LocationEntry.COLUMN_NAME_TIMESTAMP, location.getTime());
                values.put(DatabaseContract.LocationEntry.COLUMN_NAME_LATITUDE, location.getLatitude());
                values.put(DatabaseContract.LocationEntry.COLUMN_NAME_LONGITUDE, location.getLongitude());
                values.put(DatabaseContract.LocationEntry.COLUMN_NAME_ACCURACY, location.getAccuracy());

                // Insert the new row, returning the primary key value of the new row
                long newRowId = db.insert(DatabaseContract.LocationEntry.TABLE_NAME, null, values);
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i("TrackerService", "WiFi results received");

            List<ScanResult> results = wifiManager.getScanResults();
            for (ScanResult scanResult : results) {

                int channel = convertFrequencyToChannel(scanResult.frequency);

                if (channel != 1
                        || !"pjodd.se".equals(scanResult.SSID)) {
                    continue;
                }

                long timestampBootEpochMilliseconds = java.lang.System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
                long timestampScanResultEpochMilliseconds = timestampBootEpochMilliseconds + (scanResult.timestamp / 1000L);

                ContentValues values = new ContentValues();
                values.put(DatabaseContract.WiFiEntry.COLUMN_NAME_TIMESTAMP, timestampScanResultEpochMilliseconds);
                values.put(DatabaseContract.WiFiEntry.COLUMN_NAME_SSID, scanResult.SSID);
                values.put(DatabaseContract.WiFiEntry.COLUMN_NAME_BSSID, scanResult.BSSID);
                values.put(DatabaseContract.WiFiEntry.COLUMN_NAME_DBM, scanResult.level);
                values.put(DatabaseContract.WiFiEntry.COLUMN_NAME_FREQUENCY, scanResult.frequency);

                // Insert the new row, returning the primary key value of the new row
                long newRowId = db.insert(DatabaseContract.WiFiEntry.TABLE_NAME, null, values);

            }

            // immediately trigger next scan
            wifiManager.startScan();

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        databaseHelper = new DatabaseHelper(getApplicationContext());
        db = databaseHelper.getWritableDatabase();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Enabling WiFi...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "GPS access missing. Quitting.", Toast.LENGTH_LONG).show();
            return;
        }

        locationManager.addNmeaListener(nmeaListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        wifiManager.startScan();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
        locationManager.removeNmeaListener(nmeaListener);
        unregisterReceiver(wifiReceiver);
        db.close();
    }

    public static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }


}
