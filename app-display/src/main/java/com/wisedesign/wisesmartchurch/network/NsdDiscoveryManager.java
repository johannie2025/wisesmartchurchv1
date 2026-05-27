package com.wisedesign.elitecastpro.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

/**
 * Utilisé par les TV Box (mode Écran) pour découvrir automatiquement
 * le serveur EliteCast Régie sur le réseau local sans configuration.
 */
public class NsdDiscoveryManager {

    private static final String TAG = "NsdDiscovery";
    private static final String SERVICE_TYPE = "_elitecast._tcp.";

    private final Context context;
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private DiscoveryCallback callback;

    public interface DiscoveryCallback {
        void onMasterFound(String host, int wsPort, int httpPort);
        void onDiscoveryFailed(int errorCode);
    }

    public NsdDiscoveryManager(Context context) {
        this.context = context;
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void discover(DiscoveryCallback callback) {
        this.callback = callback;

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String type) {
                Log.i(TAG, "NSD Discovery démarré pour: " + type);
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.i(TAG, "Service EliteCast trouvé: " + serviceInfo.getServiceName());
                nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo s, int errorCode) {
                        Log.e(TAG, "Résolution échouée: " + errorCode);
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo s) {
                        String host = s.getHost().getHostAddress();
                        int wsPort = s.getPort();
                        int httpPort = wsPort - 1; // fallback
                        try {
                            String httpAttr = new String(s.getAttributes()
                                    .get("httpPort"));
                            httpPort = Integer.parseInt(httpAttr);
                        } catch (Exception ignored) {}

                        Log.i(TAG, "Régie trouvée → " + host +
                                " WS:" + wsPort + " HTTP:" + httpPort);
                        if (callback != null)
                            callback.onMasterFound(host, wsPort, httpPort);
                        stopDiscovery();
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.w(TAG, "Service EliteCast perdu: " + serviceInfo.getServiceName());
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "NSD Discovery arrêtée");
            }

            @Override
            public void onStartDiscoveryFailed(String type, int errorCode) {
                Log.e(TAG, "Démarrage découverte échoué: " + errorCode);
                if (callback != null) callback.onDiscoveryFailed(errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String type, int errorCode) {
                Log.e(TAG, "Arrêt découverte échoué: " + errorCode);
            }
        };

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD,
                discoveryListener);
    }

    public void stopDiscovery() {
        try {
            if (discoveryListener != null) {
                nsdManager.stopServiceDiscovery(discoveryListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur arrêt discovery: " + e.getMessage());
        }
    }
}
