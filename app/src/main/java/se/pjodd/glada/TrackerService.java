package se.pjodd.glada;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import se.pjodd.glada.db.DatabaseContract;
import se.pjodd.glada.db.DatabaseHelper;

public class TrackerService extends Service {


    public TrackerService() {
    }

    private Messenger messenger;

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    private DatabaseHelper databaseHelper;
    private SQLiteDatabase db;

    private TemporalLocationAwareWiFiManager wifiManager;

    private Grid grid = new Grid(0.003d);
    private final Map<Long, GridCellData> gridData = new HashMap<>();
    private final Map<Long, GridCellData> gridDataDelta = new HashMap<>();


    public static final int DISABLE_SERVICE = -2;
    public static final int ENABLE_SERVICE = -1;
    public static final int REQUEST_GRID_DATA = 1;
    public static final int REQUEST_GRID_DATA_DELTA = 2;

    private static class IncomingRequestHandler extends Handler {

        private TrackerService trackerService;

        private IncomingRequestHandler(TrackerService trackerService) {
            this.trackerService = trackerService;
        }

        @Override
        public void handleMessage(Message requestMessage) {

            if (requestMessage.what == ENABLE_SERVICE) {

                Message statusResponse = new Message();
                try {
                    trackerService.enable();
                    statusResponse.getData().putBoolean("enabled", trackerService.enabled.get());

                } catch (Exception e) {
                    Log.e("TrackerService", "Exception while enabling service", e);
                    statusResponse.getData().putBoolean("enabled", trackerService.enabled.get());
                    statusResponse.getData().putString("errorMessage", e.getMessage());
                }
                statusResponse.getData().putInt("gridData.size", trackerService.gridData.size());
                statusResponse.getData().putInt("gridDataDelta.size", trackerService.gridDataDelta.size());

                try {
                    requestMessage.replyTo.send(statusResponse);
                } catch (RemoteException re) {
                    Log.e("TrackerService", "Unable to send status response after enabled request", re);
                }

            } else if (requestMessage.what == DISABLE_SERVICE) {
                Message statusResponse = new Message();
                try {
                    trackerService.disable();
                    statusResponse.getData().putBoolean("enabled", trackerService.enabled.get());

                } catch (Exception e) {
                    Log.e("TrackerService", "Exception while disabling service", e);
                    statusResponse.getData().putBoolean("enabled", trackerService.enabled.get());
                    statusResponse.getData().putString("errorMessage", e.getMessage());
                }
                statusResponse.getData().putInt("gridData.size", trackerService.gridData.size());
                statusResponse.getData().putInt("gridDataDelta.size", trackerService.gridDataDelta.size());

                try {
                    requestMessage.replyTo.send(statusResponse);
                } catch (RemoteException re) {
                    Log.e("TrackerService", "Unable to send status response after disable request", re);
                }

            } else if (requestMessage.what == REQUEST_GRID_DATA) {
                synchronized (trackerService.gridData) {
                    for (Map.Entry<Long, GridCellData> entry : trackerService.gridData.entrySet()) {
                        Message gridCellMessage = new Message();
                        gridCellMessage.getData().putLong("cellIdentity", entry.getKey());
                        gridCellMessage.getData().putLong("timestamp", entry.getValue().getTimestamp());
                        gridCellMessage.getData().putFloat("accuracy", entry.getValue().getAccuracy());
                        gridCellMessage.getData().putInt("dBm", entry.getValue().getdBm());
                        gridCellMessage.getData().putDouble("pdop", entry.getValue().getPdop());
                        try {
                            requestMessage.replyTo.send(gridCellMessage);
                        } catch (RemoteException re) {
                            Log.e("TrackerService", "Unable to send grid data message", re);
                        }
                    }
                    trackerService.gridDataDelta.clear();
                }

            } else if (requestMessage.what == REQUEST_GRID_DATA_DELTA) {
                synchronized (trackerService.gridDataDelta) {
                    for (Iterator<Map.Entry<Long, GridCellData>> iterator = trackerService.gridDataDelta.entrySet().iterator(); iterator.hasNext(); ) {
                        Map.Entry<Long, GridCellData> entry = iterator.next();
                        Message gridCellMessage = new Message();
                        gridCellMessage.getData().putLong("cellIdentity", entry.getKey());
                        gridCellMessage.getData().putLong("timestamp", entry.getValue().getTimestamp());
                        gridCellMessage.getData().putFloat("accuracy", entry.getValue().getAccuracy());
                        gridCellMessage.getData().putInt("dBm", entry.getValue().getdBm());
                        gridCellMessage.getData().putDouble("pdop", entry.getValue().getPdop());
                        try {
                            requestMessage.replyTo.send(gridCellMessage);
                            iterator.remove();
                        } catch (android.os.RemoteException re) {
                            Log.e("TrackerService", "Unable to send grid data delta message", re);
                        }
                    }
                }
            }
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        messenger = new Messenger(new IncomingRequestHandler(this));
        databaseHelper = new DatabaseHelper(getApplicationContext());

        // load grid data from database
        {
            db = databaseHelper.getWritableDatabase();
            String[] projection = {
                    DatabaseContract.Entry.COLUMN_NAME_TIMESTAMP,
                    DatabaseContract.Entry.COLUMN_NAME_LATITUDE,
                    DatabaseContract.Entry.COLUMN_NAME_LONGITUDE,
                    DatabaseContract.Entry.COLUMN_NAME_ACCURACY,
                    DatabaseContract.Entry.COLUMN_NAME_PDOP,
                    DatabaseContract.Entry.COLUMN_NAME_DBM,
            };

            String sortOrder = DatabaseContract.Entry.COLUMN_NAME_TIMESTAMP + " DESC";
            Cursor cursor = db.query(
                    DatabaseContract.Entry.TABLE_NAME,        // The table to query
                    projection,                               // The columns to return
                    null,                                     // The columns for the WHERE clause
                    null,                                     // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    sortOrder                                 // The sort order
            );

            while(cursor.moveToNext()) {
                updateGridData(
                        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.Entry.COLUMN_NAME_TIMESTAMP)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.Entry.COLUMN_NAME_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.Entry.COLUMN_NAME_LONGITUDE)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseContract.Entry.COLUMN_NAME_ACCURACY)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.Entry.COLUMN_NAME_PDOP)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.Entry.COLUMN_NAME_DBM))
                );
            }
            cursor.close();

            db.close();
        }

//        enable();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void enable() {

        synchronized (enabled) {

            if (enabled.get()) {
                return;
            }

            db = databaseHelper.getWritableDatabase();
            try {
                wifiManager = new TemporalLocationAwareWiFiManager(getApplicationContext()) {
                    @Override
                    public boolean accept(long timestamp, ScanResult scanResult) {
                        return (TemporalLocationAwareWiFiManager.convertFrequencyToChannel(scanResult.frequency) == 1
                                && "pjodd.se".equals(scanResult.SSID));
                    }

                    @Override
                    public void onReceive(long timestamp, ScanResult scanResult, TemporalLocationManager.Position position) {

                        // store in database
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
                        db.insert(DatabaseContract.Entry.TABLE_NAME, null, values);

                        // add to grid and prepare to send to client
                        updateGridData(timestamp, position.getLatitude(), position.getLongitude(), position.getAccuracy(), position.getPdop(), scanResult.level);
                    }
                };

                if (!wifiManager.start()) {
                    throw new RuntimeException("Unable to start location aware WiFi manager!");
                }
            } catch (Exception e) {
                db.close();
                throw e;
            }

            enabled.set(true);
        }
    }

    private void disable() {

        synchronized (enabled) {

            if (!enabled.get()) {
                return;
            }

            wifiManager.stop();
            db.close();

            enabled.set(false);
        }
    }

    private void updateGridData(long timestamp, double latitude, double longitude, float accuracy, double pdop, int dBm) {
        Grid.Cell cell = grid.getCell(latitude, longitude);
        synchronized (gridData) {
            GridCellData cellData = gridData.get(cell.getIdentity());
            if (cellData == null) {
                cellData = new GridCellData();
                cellData.setAccuracy(accuracy);
                cellData.setdBm(dBm);
                cellData.setTimestamp(timestamp);
                cellData.setPdop(pdop);
                gridData.put(cell.getIdentity(), cellData);
                synchronized (gridDataDelta) {
                    gridDataDelta.put(cell.getIdentity(), cellData);
                }
            } else if (cellData.getAccuracy() >= accuracy) {
                cellData.setAccuracy(accuracy);
                cellData.setdBm(dBm);
                cellData.setTimestamp(timestamp);
                cellData.setPdop(pdop);
                synchronized (gridDataDelta) {
                    gridDataDelta.put(cell.getIdentity(), cellData);
                }
            }
        }
    }

}
