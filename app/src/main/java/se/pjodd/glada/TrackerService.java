package se.pjodd.glada;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;

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

    private Grid grid = new Grid(0.01d);
    private Map<Long, GridCellData> data = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();

        databaseHelper = new DatabaseHelper(getApplicationContext());
        db = databaseHelper.getWritableDatabase();

        wifiManager = new TemporalLocationAwareWiFiManager(getApplicationContext()) {
            @Override
            public boolean accept(long timestamp, ScanResult scanResult) {
                return (TemporalLocationAwareWiFiManager.convertFrequencyToChannel(scanResult.frequency) == 1
                        && "pjodd.se".equals(scanResult.SSID));
            }

            @Override
            public void onReceive(long timestamp, ScanResult scanResult, TemporalLocationManager.Position position) {
                Grid.Cell cell = grid.getCell(position.getLatitude(), position.getLongitude());
                GridCellData cellData = data.get(cell.getIdentity());
                if (cellData == null) {
                    cellData = new GridCellData();
                    cellData.setAccuracy(position.getAccuracy());
                    cellData.setdBm(scanResult.level);
                    cellData.setTimestamp(timestamp);
                    cellData.setPdop(position.getPdop());
                    data.put(cell.getIdentity(), cellData);
                } else if (cellData.getAccuracy() >= position.getAccuracy()) {
                    cellData.setAccuracy(position.getAccuracy());
                    cellData.setdBm(scanResult.level);
                    cellData.setTimestamp(timestamp);
                    cellData.setPdop(position.getPdop());
                }
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



}
