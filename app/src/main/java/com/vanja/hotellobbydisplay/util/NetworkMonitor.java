package com.vanja.hotellobbydisplay.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * One-shot check for "does the device have working internet right now?" (APV-25).
 *
 * <p>Needs the {@code ACCESS_NETWORK_STATE} permission. Uses
 * {@code NET_CAPABILITY_VALIDATED} so a Wi-Fi that is connected but has no
 * actual internet (captive portal, router with no WAN) counts as offline.</p>
 */
public final class NetworkMonitor {

    private NetworkMonitor() {
        // Utility class - never instantiated.
    }

    /** @return true if there is an active network that actually reaches the internet. */
    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        Network network = cm.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
