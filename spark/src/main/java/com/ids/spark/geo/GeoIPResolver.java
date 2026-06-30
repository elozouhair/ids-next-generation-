package com.ids.spark.geo;

import com.typesafe.config.Config;
import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class GeoIPResolver {

    private final File dbFile;
    private final double defaultLat;
    private final double defaultLon;
    private final Object mmdbReader;
    private boolean useMmdb;

    public GeoIPResolver(Config config) {
        String dbPath = config.getString("geoip.db-path");
        this.dbFile = new File(dbPath);
        this.defaultLat = config.getDouble("geoip.default-latitude");
        this.defaultLon = config.getDouble("geoip.default-longitude");

        Object reader = null;
        boolean valid = false;
        if (dbFile.exists()) {
            try {
                Class<?> cls = Class.forName("com.maxmind.geoip2.DatabaseReader");
                reader = cls.getConstructor(File.class).newInstance(dbFile);
                valid = true;
            } catch (Exception e) {
                System.out.println("=== GeoIP: MaxMind DB present but failed to load: " + e.getMessage());
            }
        }
        this.mmdbReader = reader;
        this.useMmdb = valid;
        System.out.println("=== GeoIP: " + (valid ? "MaxMind DB loaded from " + dbPath : "using hash-based fallback"));
    }

    public double[] resolve(String ip) {
        if (useMmdb && mmdbReader != null) {
            try {
                Class<?> cityResponseClass = Class.forName("com.maxmind.geoip2.model.CityResponse");
                java.lang.reflect.Method cityMethod = mmdbReader.getClass().getMethod("city", InetAddress.class);
                Object response = cityMethod.invoke(mmdbReader, InetAddress.getByName(ip));
                java.lang.reflect.Method getLocation = cityResponseClass.getMethod("getLocation");
                Object location = getLocation.invoke(response);
                double lat = (double) location.getClass().getMethod("getLatitude").invoke(location);
                double lon = (double) location.getClass().getMethod("getLongitude").invoke(location);
                return new double[]{lat, lon};
            } catch (Exception e) {
                return hashFallback(ip);
            }
        }
        return hashFallback(ip);
    }

    private double[] hashFallback(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            byte[] bytes = addr.getAddress();
            int hash = 0;
            for (byte b : bytes) {
                hash = 31 * hash + (b & 0xFF);
            }
            double lat = defaultLat + (hash % 7000) / 1000.0;
            double lon = defaultLon + ((hash / 7000) % 7000) / 1000.0;
            if (lat > 90) lat = 90 - (lat - 90);
            if (lat < -90) lat = -90 + (-90 - lat);
            if (lon > 180) lon = 180 - (lon - 180);
            if (lon < -180) lon = -180 + (-180 - lon);
            return new double[]{lat, lon};
        } catch (Exception e) {
            return new double[]{defaultLat, defaultLon};
        }
    }
}