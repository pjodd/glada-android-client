package se.pjodd.glada;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
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
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static se.pjodd.glada.R.id.map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private boolean pjoddInRange = false;

    private WifiManager wifiManager;

    private LocationManager locationManager;

    private GoogleMap googleMap;
    private Circle locationCircle;

    private ProgressBar pdopMeter;
    private ProgressBar accuracyMeter;

    private Toolbar toolBar;

    private AtomicBoolean trackingServiceEnabled = new AtomicBoolean(false);

    private void setPdopMeter(Double pdop) {

        if (pdop == null) {
            pdop = 100d;
        }
        pdopMeter.setProgress((int) (pdop * 100d));
        if (pdop >= 4.5) {
            pdopMeter.getProgressDrawable().setColorFilter(Gradient.TEN_GREEN_YELLOW_RED[9], android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 4) {
            pdopMeter.getProgressDrawable().setColorFilter(Gradient.TEN_GREEN_YELLOW_RED[8], android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 3.5) {
            pdopMeter.getProgressDrawable().setColorFilter(Gradient.TEN_GREEN_YELLOW_RED[7], android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 3) {
            pdopMeter.getProgressDrawable().setColorFilter(Gradient.TEN_GREEN_YELLOW_RED[6], android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 2.5) {
            pdopMeter.getProgressDrawable().setColorFilter(Gradient.TEN_GREEN_YELLOW_RED[5], android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 2) {
            pdopMeter.getProgressDrawable().setColorFilter(Gradient.TEN_GREEN_YELLOW_RED[4], android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 1.5) {
            pdopMeter.getProgressDrawable().setColorFilter(Gradient.TEN_GREEN_YELLOW_RED[3], android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 1) {
            pdopMeter.getProgressDrawable().setColorFilter(Gradient.TEN_GREEN_YELLOW_RED[2], android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (pdop >= 0.5) {
            pdopMeter.getProgressDrawable().setColorFilter(Gradient.TEN_GREEN_YELLOW_RED[1], android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            pdopMeter.getProgressDrawable().setColorFilter(Gradient.TEN_GREEN_YELLOW_RED[0], android.graphics.PorterDuff.Mode.SRC_IN);
        }

    }

    private void setAccuracyMeter(Float accuracyMeters) {

        int accuracy;
        if (accuracyMeters == null) {
            accuracy = Integer.MAX_VALUE;
        } else {
            accuracy = accuracyMeters.intValue();
        }

        if (accuracy > accuracyMeter.getMax()) {
            accuracy = accuracyMeter.getMax();
        }

        accuracyMeter.setProgress(accuracy);
        accuracyMeter.getProgressDrawable().setColorFilter(Gradient.TEN_GREEN_YELLOW_RED[accuracy - 1], android.graphics.PorterDuff.Mode.SRC_IN);


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
            updateLocation(location);
            setAccuracyMeter(location.getAccuracy());
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

            boolean updateLocationCircle = MapActivity.this.pjoddInRange != pjoddInRange;
            MapActivity.this.pjoddInRange = pjoddInRange;
            if (updateLocationCircle) {
                updateLocation(lastKnownLocation);
            }
            // todo this should never be null!!! but it is
            if (trackerServiceMessenger != null) {
                Message message = Message.obtain(null, TrackerService.REQUEST_GRID_DATA_DELTA, 1, 1);
                message.replyTo = gridDataMessenger;
                try {
                    trackerServiceMessenger.send(message);
                } catch (android.os.RemoteException re) {
                    Log.e("MapActivity", "Unable to request grid data delta", re);
                }
            }

        }


    };

    private Location lastKnownLocation;

    private void updateLocation(Location location) {

        if (location == null) {
            return;
        }

        if (googleMap != null) {

            if (lastKnownLocation == null) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to malmö
                        .zoom(17)                   // Sets the zoom
                        .zoom(17)                   // Sets the zoom
                        .bearing(90)                // Sets the orientation of the camera to east
                        .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }

            if (locationCircle != null) {
                locationCircle.remove();
            }

            CircleOptions circleOptions = new CircleOptions()
                    .center(new LatLng(location.getLatitude(), location.getLongitude()))
                    .radius(location.getAccuracy())
                    .strokeColor(Color.BLACK);

            if (pjoddInRange) {
                circleOptions.fillColor(0x5500ff00);
            } else {
                circleOptions.fillColor(0x55ff0000);
            }

            locationCircle = googleMap.addCircle(circleOptions);
        }

        lastKnownLocation = location;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        pdopMeter = (ProgressBar) findViewById(R.id.pdopMeter);
        pdopMeter.setMax(500); // todo from settings
        setPdopMeter(null);

        accuracyMeter = (ProgressBar) findViewById(R.id.accuracyMeter);
        accuracyMeter.setMax(10); // todo from settings
        setAccuracyMeter(null);

        toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        Intent intent = new Intent(this, TrackerService.class);
        bindService(intent, trackerServiceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        pjoddInRange = false;

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

        updateLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(trackerServiceConnection);
            isBound = false;
        }
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
        this.googleMap = googleMap;

        if (lastKnownLocation != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()))      // Sets the center of the map to malmö
                    .zoom(17)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
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

    private Messenger trackerServiceMessenger;
    private boolean isBound;
    private ServiceConnection trackerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;
            trackerServiceMessenger = new Messenger(service);
            requestAllGridData();
        }
    };

    private Messenger trackingServiceStatusMessenger = new Messenger(new TrackingServiceStatusHandler(this));

    private static class TrackingServiceStatusHandler extends Handler {
        private MapActivity mapActivity;

        public TrackingServiceStatusHandler(MapActivity mapActivity) {
            this.mapActivity = mapActivity;
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            mapActivity.trackingServiceEnabled.set(message.getData().getBoolean("enabled"));
            String errorMessage = message.getData().getString("errorMessage");
            if (errorMessage != null) {
                // todo
            }
            MenuItem trackingMenuItem = mapActivity.toolBar.getMenu().findItem(R.id.action_enable_disable_tracking);
            if (mapActivity.trackingServiceEnabled.get()) {
                trackingMenuItem.setTitle("Disable tracking");
            } else {
                trackingMenuItem.setTitle("Enable tracking");
            }

        }
    }

    private Messenger gridDataMessenger = new Messenger(new GridDataHandler(this));

    private static class GridDataHandler extends Handler {

        private MapActivity mapActivity;

        public GridDataHandler(MapActivity mapActivity) {
            this.mapActivity = mapActivity;
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            long cellIdentity = message.getData().getLong("cellIdentity");
            long timestamp = message.getData().getLong("timestamp");
            float accuracy = message.getData().getFloat("accuracy");
            int dBm = message.getData().getInt("dBm");
            double pdop = message.getData().getDouble("pdop");

            GridCellData cellData = mapActivity.gridData.get(cellIdentity);
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
                mapActivity.gridData.put(cellIdentity, cellData);
            }

            Grid.Cell cell = mapActivity.grid.getCell(cellIdentity);
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
            cellData.setPolygon(mapActivity.googleMap.addPolygon(polygonOptions));
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
            trackerServiceMessenger.send(message);
        } catch (android.os.RemoteException re) {
            Log.e("MapActivity", "Unable to request grid data", re);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);//Menu Resource, Menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_enable_disable_tracking:
                Message message;
                if (trackingServiceEnabled.get()) {
                    message = Message.obtain(null, TrackerService.DISABLE_SERVICE, 1, 1);
                } else {
                    message = Message.obtain(null, TrackerService.ENABLE_SERVICE, 1, 1);
                }
                message.replyTo = trackingServiceStatusMessenger;
                try {
                    trackerServiceMessenger.send(message);
                } catch (RemoteException re) {
                    Log.e("MapActivity", "Unable to send enable/disable tracking service", re);
                }
                return true;

            case R.id.action_zoom_to_my_location:
                if (lastKnownLocation != null) {
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()))      // Sets the center of the map to malmö
                            .zoom(17)                   // Sets the zoom
                            .zoom(17)                   // Sets the zoom
                            .bearing(90)                // Sets the orientation of the camera to east
                            .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                            .build();                   // Creates a CameraPosition from the builder
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

}
