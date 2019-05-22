package cameraopencv.java.dji.com.utils;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geometry.Point;
import com.google.maps.android.projection.SphericalMercatorProjection;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;

/**
 * Created by dji on 15/12/18.
 */

public class GeneralUtils {
    public static final double ONE_METER_OFFSET = 0.00000899322;
    private static long lastClickTime;

    private static SphericalMercatorProjection projection = new SphericalMercatorProjection(1.0D);

  //  private static final double r =6378137;

  //  public static final double ONE_METER_LAT = (1.00 / r) * (180.0 / Math.PI);
   // public static final double ONE_METER_LONG = (1.0 / r) * (180.0 / Math.PI) / Math.cos((lat0 * Math.PI/180.0));

    public static boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (0 < timeD && timeD < 800) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

    public static boolean checkGpsCoordinate(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f
                && longitude != 0f);
    }

    public static double toRadian(double x) {
        return x * Math.PI / 180.0;
    }

    public static double toDegree(double x) {
        return x * 180 / Math.PI;
    }

    public static double cosForDegree(double degree) {
        return Math.cos(degree * Math.PI / 180.0f);
    }

    public static double calcLongitudeOffset(double latitude) {
        return ONE_METER_OFFSET / cosForDegree(latitude);
    }

    public static void addLineToSB(StringBuffer sb, String name, Object value) {
        if (sb == null) return;
        sb.
                append(name == null ? "" : name + ": ").
                append(value == null ? "" : value + "").
                append("\n");
    }

    public static CommonCallbacks.CompletionCallback getCommonCompletionCallback() {
        return new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                ToastUtils.setResultToToast(djiError == null ? "Succeed!" : "failed!" + djiError.getDescription());
            }
        };
    }


    public static LatLng pointToLatLng(Point p) {
        return projection.toLatLng(p);
    }


}
