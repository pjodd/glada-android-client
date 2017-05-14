package se.pjodd.glada;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.pjodd.glada.db.DatabaseHelper;

import static se.pjodd.glada.R.id.map;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private boolean pjoddInRange = false;

    private DatabaseHelper databaseHelper;
    private SQLiteDatabase db;

    private WifiManager wifiManager;

    private LocationManager locationManager;

    private GoogleMap mMap;
    private Circle locationCircle;

    private ProgressBar pdopMeter;
    private TextView pdopLabel;

    private TextView accuracyLabel;

    private void setPdopMeter(Double pdop) {

        pdopLabel.setText("pdop: " + String.valueOf(pdop));

        if (pdop == null) {
            pdop = 100d;
        }
        pdopMeter.setProgress((int) (pdop * 100d));
        if (pdop >= 5) {
            pdopMeter.getProgressDrawable().setColorFilter(Color.rgb(0xff, 0x00, 0x00), android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 4) {
            pdopMeter.getProgressDrawable().setColorFilter(Color.rgb(0xff, 0x66, 0x00), android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 3) {
            pdopMeter.getProgressDrawable().setColorFilter(Color.rgb(0xff, 0xcc, 0x00), android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 2) {
            pdopMeter.getProgressDrawable().setColorFilter(Color.rgb(0xcb, 0xff, 0x00), android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 1) {
            pdopMeter.getProgressDrawable().setColorFilter(Color.rgb(0x65, 0xff, 0x00), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            pdopMeter.getProgressDrawable().setColorFilter(Color.rgb(0x00, 0xff, 0x00), android.graphics.PorterDuff.Mode.SRC_IN);
        }

    }

    private NmeaParserListener nmeaListener = new NmeaParserListener() {
        @Override
        public void onNmeaReceived(long timestamp, String nmeaSentence) {
            super.onNmeaReceived(timestamp, nmeaSentence);
            setPdopMeter(getPdop());
        }
    };

    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            accuracyLabel.setText("accuracy: " + String.valueOf(location.getAccuracy()));

            lastKnownLocation = location;
            updateLocationCircle();

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

            boolean pjoddInRange = false;

            List<ScanResult> results = wifiManager.getScanResults();
            for (ScanResult scanResult : results) {

                int channel = convertFrequencyToChannel(scanResult.frequency);

                if (channel != 1
                        || !"pjodd.se".equals(scanResult.SSID)) {
                    continue;
                }

                pjoddInRange = true;
                break;

            }

            boolean updateLocationCircle = MainActivity.this.pjoddInRange != pjoddInRange;
            MainActivity.this.pjoddInRange = pjoddInRange;
            if (updateLocationCircle) {
                updateLocationCircle();
            }

            Message message = Message.obtain(null, TrackerService.REQUEST_GRID_DATA_DELTA, 1, 1);
            message.replyTo = gridDataMessenger;
            try {
                msgService.send(message);
            } catch (android.os.RemoteException re) {
                Log.e("MainActivity", "Unable to request grid data delta", re);
            }

        }


    };

    private Location lastKnownLocation;

    private void updateLocationCircle() {
        updateLocationCircle(lastKnownLocation);
    }

    private void updateLocationCircle(Location location) {

        if (mMap != null) {
            if (locationCircle != null) {
                locationCircle.remove();
            }

            CircleOptions circleOptions = new CircleOptions()
                    .center(new LatLng(location.getLatitude(), location.getLongitude()))
                    .radius(location.getAccuracy())
                    .strokeColor(Color.BLACK);

            // 0x represents, this is an hexadecimal code
            // 55 represents percentage of transparency. For 100% transparency, specify 00.
            // For 0% transparency ( ie, opaque ) , specify ff
            // The remaining 6 characters(00ff00) specify the fill color
            if (pjoddInRange) {
                circleOptions.fillColor(0x5500ff00);
            } else {
                circleOptions.fillColor(0x55ff0000);
            }

            locationCircle = mMap.addCircle(circleOptions);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        accuracyLabel = (TextView) findViewById(R.id.accuracyLabel);

        pdopLabel = (TextView) findViewById(R.id.pdopLabel);

        pdopMeter = (ProgressBar) findViewById(R.id.pdopMeter);
        pdopMeter.setMax(500);
        setPdopMeter(null);

        Intent intent = new Intent(this, TrackerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        pjoddInRange = false;

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

        lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        updateLocationCircle();

        locationManager.addNmeaListener(nmeaListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);

        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();



    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(locationListener);
        locationManager.removeNmeaListener(nmeaListener);
        unregisterReceiver(wifiReceiver);
        db.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        updateLocationCircle();

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), 13));

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()))      // Sets the center of the map to location user
                .zoom(17)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


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

    private Messenger msgService;
    private boolean isBound;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;
            msgService = new Messenger(service);

            requestAllGridData();
        }
    };

    Messenger gridDataMessenger = new Messenger(new GridDataHandler(this));

    private static class GridDataHandler extends Handler {

        private MainActivity mainActivity;

        public GridDataHandler(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            long cellIdentity = msg.getData().getLong("cellIdentity");
            long timestamp = msg.getData().getLong("timestamp");
            float accuracy = msg.getData().getFloat("accuracy");
            int dBm = msg.getData().getInt("dBm");
            double pdop = msg.getData().getDouble("pdop");

            GridCellData cellData = mainActivity.gridData.get(cellIdentity);
            if (cellData != null) {
                cellData.getPolygon().remove();
                cellData.setTimestamp(timestamp);
                cellData.setAccuracy(accuracy);
                cellData.setdBm(dBm);
                cellData.setPdop(pdop);
            } else {
                cellData = new GridCellData();
                cellData.setTimestamp(timestamp);
                cellData.setAccuracy(accuracy);
                cellData.setdBm(dBm);
                cellData.setPdop(pdop);
                mainActivity.gridData.put(cellIdentity, cellData);
            }

            Grid.Cell cell = mainActivity.grid.getCell(cellIdentity);
            Grid.Envelope envelope = cell.getEnvelope();

            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.add(
                    new LatLng(envelope.getSouthwest().getLatitude(), envelope.getSouthwest().getLongitude()),
                    new LatLng(envelope.getNortheast().getLatitude(), envelope.getSouthwest().getLongitude()),
                    new LatLng(envelope.getNortheast().getLatitude(), envelope.getNortheast().getLongitude()),
                    new LatLng(envelope.getSouthwest().getLatitude(), envelope.getNortheast().getLongitude()),
                    new LatLng(envelope.getSouthwest().getLatitude(), envelope.getSouthwest().getLongitude())
            );
            int color = Gradient.TEN_RED_YELLOW_GREEN[WifiManager.calculateSignalLevel(cellData.getdBm(), 10)];
            polygonOptions.fillColor(color);
            polygonOptions.strokeColor(color);
            polygonOptions.strokeWidth(1); // px
            cellData.setPolygon(mainActivity.mMap.addPolygon(polygonOptions));
        }
    }

    private Grid grid = new Grid(0.003d);
    private Map<Long, GridCellData> gridData = new HashMap<>();

    private static class GridCellData extends se.pjodd.glada.GridCellData {
        private Polygon polygon;

        public Polygon getPolygon() {
            return polygon;
        }

        public void setPolygon(Polygon polygon) {
            this.polygon = polygon;
        }
    }

    private void requestAllGridData() {
        Message message = Message.obtain(null, TrackerService.REQUEST_GRID_DATA, 1, 1);
        message.replyTo = gridDataMessenger;
        try {
            msgService.send(message);
        } catch (android.os.RemoteException re) {
            Log.e("MainActivity", "Unable to request grid data", re);
        }
    }
}
