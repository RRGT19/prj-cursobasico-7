package com.example.robertgomez.proyectofinalconnect4;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Detect if there is internet connection available or not
 */
public class NetworkUtils {

    private static int TYPE_WIFI = 1;
    private static int TYPE_MOBILE = 2;
    private static int TYPE_NOT_CONNECTED = 0;

    private static int getConnectivityStatus(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                    && networkInfo.getState() == NetworkInfo.State.CONNECTED) {

                return TYPE_WIFI;

            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE
                    && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                return TYPE_MOBILE;
            }
        }

        return TYPE_NOT_CONNECTED;
    }

    public static boolean isNetworkConnected(Context context) {
        int networkStatus = getConnectivityStatus(context);

        if (networkStatus == TYPE_WIFI || networkStatus == TYPE_MOBILE) return true;
        else return false;
    }
}