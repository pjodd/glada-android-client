package se.pjodd.glada;

import android.location.GpsStatus;

/**
 * @author kalle
 * @since 2017-05-13 05:00
 */

public class NmeaParserListener implements GpsStatus.NmeaListener {

    private Double hdop;
    private Double pdop;
    private Double vdop;
    private String geoIdHeight;
    private String ageOfDgpsData;
    private String dgpsId;

    private boolean isNullOrEmpty(String input) {
        return input == null || input.isEmpty();
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmeaSentence) {
        if(isNullOrEmpty(nmeaSentence)){
            return;
        }

//        hdop = null;
//        pdop = null;
//        vdop = null;
//        geoIdHeight = null;
//        ageOfDgpsData = null;
//        dgpsId = null;

        String[] nmeaParts = nmeaSentence.split(",");

        if (nmeaParts[0].equalsIgnoreCase("$GPGSA")) {

            if (nmeaParts.length > 15 && !isNullOrEmpty(nmeaParts[15])) {
                pdop = Double.parseDouble(nmeaParts[15]);
            }

            if (nmeaParts.length > 16 &&!isNullOrEmpty(nmeaParts[16])) {
                hdop = Double.parseDouble(nmeaParts[16]);
            }

            if (nmeaParts.length > 17 &&!isNullOrEmpty(nmeaParts[17]) && !nmeaParts[17].startsWith("*")) {
                vdop = Double.parseDouble(nmeaParts[17].split("\\*")[0]);
            }
        }


        if (nmeaParts[0].equalsIgnoreCase("$GPGGA")) {
            if (nmeaParts.length > 8 &&!isNullOrEmpty(nmeaParts[8])) {
                hdop = Double.parseDouble(nmeaParts[8]);
            }
            if (nmeaParts.length > 11 &&!isNullOrEmpty(nmeaParts[11])) {
                geoIdHeight = nmeaParts[11];
            }
            if (nmeaParts.length > 13 &&!isNullOrEmpty(nmeaParts[13])) {
                ageOfDgpsData = nmeaParts[13];
            }
            if (nmeaParts.length > 14 &&!isNullOrEmpty(nmeaParts[14]) && !nmeaParts[14].startsWith("*")) {
                dgpsId = nmeaParts[14].split("\\*")[0];
            }
        }

    }

    public Double getHdop() {
        return hdop;
    }

    public Double getPdop() {
        return pdop;
    }

    public Double getVdop() {
        return vdop;
    }

    public String getGeoIdHeight() {
        return geoIdHeight;
    }

    public String getAgeOfDgpsData() {
        return ageOfDgpsData;
    }

    public String getDgpsId() {
        return dgpsId;
    }
}
