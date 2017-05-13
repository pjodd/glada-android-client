package se.pjodd.glada;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.SystemClock;

import java.util.List;

/**
 * @author kalle
 * @since 2017-05-13 07:07
 */

public class TemporalLocationAwareWiFiManager {

    private Context context;
    private WifiManager wifiManager;
    private TemporalLocationManager locationManager;

    private long timestampScanningStarted = Long.MIN_VALUE;

    public TemporalLocationAwareWiFiManager(Context context) {
        this.context = context;
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        locationManager = new TemporalLocationManager(context);
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            long timestampScanResultsReceived = System.currentTimeMillis();
            long millisecondsScanning = timestampScanResultsReceived - timestampScanningStarted;
            long timestampBestGuess = (millisecondsScanning / 2) + timestampScanningStarted;

            // so the scan result timestamp is no way to be trusted.
            // sometimes it's right on spot, sometimes it reports days in the past, sometimes weeks in the future.
            // so we scan as often as we can and keep track of during what time we did that.

            List<ScanResult> results = wifiManager.getScanResults();
            for (ScanResult scanResult : results) {

                if (!accept(timestampBestGuess, scanResult)) {
                    continue;
                }

                TemporalLocationManager.Position position = locationManager.getLocation(timestampBestGuess);
                if (position != null) {
                    TemporalLocationAwareWiFiManager.this.onReceive(timestampBestGuess, scanResult, position);
                }
            }

            // immediately trigger next scan
            timestampScanningStarted = System.currentTimeMillis();
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
    public boolean accept(long timestamp, ScanResult scanResult) {
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
        timestampScanningStarted = System.currentTimeMillis();
        wifiManager.startScan();


        return true;
    }

    public void stop() {
        context.unregisterReceiver(wifiReceiver);
        locationManager.stop();
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
