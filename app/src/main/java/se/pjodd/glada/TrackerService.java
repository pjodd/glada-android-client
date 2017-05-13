package se.pjodd.glada;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.os.IBinder;

import se.pjodd.glada.db.DatabaseContract;
import se.pjodd.glada.db.DatabaseHelper;

public class TrackerService extends Service {


    public TrackerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private DatabaseHelper databaseHelper;
    private SQLiteDatabase db;

    private TemporalLocationAwareWiFiManager wifiManager;


    @Override
    public void onCreate() {
        super.onCreate();

        databaseHelper = new DatabaseHelper(getApplicationContext());
        db = databaseHelper.getWritableDatabase();

        wifiManager = new TemporalLocationAwareWiFiManager(getApplicationContext()) {
            @Override
            public boolean filterReceive(long timestamp, ScanResult scanResult) {
                return (convertFrequencyToChannel(scanResult.frequency) == 1
                        && "pjodd.se".equals(scanResult.SSID));
            }

            @Override
            public void onReceive(long timestamp, ScanResult scanResult, TemporalLocationManager.Position position) {
                ContentValues values = new ContentValues();
                values.put(DatabaseContract.Entry.COLUMN_NAME_TIMESTAMP, timestamp);
                values.put(DatabaseContract.Entry.COLUMN_NAME_SSID, scanResult.SSID);
                values.put(DatabaseContract.Entry.COLUMN_NAME_BSSID, scanResult.BSSID);
                values.put(DatabaseContract.Entry.COLUMN_NAME_DBM, scanResult.level);
                values.put(DatabaseContract.Entry.COLUMN_NAME_FREQUENCY, scanResult.frequency);
                values.put(DatabaseContract.Entry.COLUMN_NAME_LATITUDE, position.getLatitude());
                values.put(DatabaseContract.Entry.COLUMN_NAME_LONGITUDE, position.getLongitude());
                values.put(DatabaseContract.Entry.COLUMN_NAME_ACCURACY, position.getAccuracy());
                values.put(DatabaseContract.Entry.COLUMN_NAME_PDOP, position.getPdop());

                // Insert the new row, returning the primary key value of the new row
                long newRowId = db.insert(DatabaseContract.Entry.TABLE_NAME, null, values);

            }
        };

        if (!wifiManager.start()) {
            throw new RuntimeException("Unable to start location aware WiFi manager!");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wifiManager.stop();
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
