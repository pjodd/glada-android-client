package se.pjodd.glada;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.List;

/**
 * @author kalle
 * @since 2017-05-13 07:07
 */

public class TemporalLocationAwareWiFiManager {

    private Context context;
    private WifiManager wifiManager;
    private TemporalLocationManager locationManager;

    public TemporalLocationAwareWiFiManager(Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        locationManager = new TemporalLocationManager(context);
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            List<ScanResult> results = wifiManager.getScanResults();
            for (ScanResult scanResult : results) {

                long timestampBootEpochMilliseconds = java.lang.System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
                long timestampScanResultEpochMilliseconds = timestampBootEpochMilliseconds + (scanResult.timestamp / 1000L);

                if (!filterReceive(timestampScanResultEpochMilliseconds, scanResult)) {
                    continue;
                }

                TemporalLocationManager.Position position = locationManager.getLocation(timestampScanResultEpochMilliseconds);
                if (position != null) {
                    TemporalLocationAwareWiFiManager.this.onReceive(timestampScanResultEpochMilliseconds, scanResult, position);
                }
            }

            // immediately trigger next scan
            wifiManager.startScan();

        }
    };

    /**
     * Allows for filtering out scan results prior to looking up their location,
     * as looking up location is a relatively expensive and synchronized operation.
     *
     * @param timestamp
     * @param scanResult
     * @return true if the scan result is to be considered for temporal location lookup
     */
    public boolean filterReceive(long timestamp, ScanResult scanResult) {
        return true;
    }

    public void onReceive(long timestamp, ScanResult scanResult, TemporalLocationManager.Position position) {

    }

    public boolean start() {
        if (!wifiManager.isWifiEnabled()
                && !wifiManager.setWifiEnabled(true)) {
            return false;
        }

        if (!locationManager.start()) {
            return false;
        }

        context.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();


        return true;
    }

    public void stop() {
        context.unregisterReceiver(wifiReceiver);
        locationManager.stop();
    }

}
