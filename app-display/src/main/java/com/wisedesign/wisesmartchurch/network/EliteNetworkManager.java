package com.wisedesign.elitecastpro.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wisedesign.elitecastpro.data.model.BroadcastPacket;
import com.wisedesign.elitecastpro.data.model.ScreenConfig;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ═══════════════════════════════════════════════════════════════
 *  EliteNetworkManager – Cœur réseau EliteCast Pro
 *
 *  Gère simultanément :
 *  ● Serveur HTTP (NanoHTTPD) port 8080  → Interface Web de régie
 *  ● Serveur WebSocket (Java-WS) port 8081 → Commandes temps réel
 *  ● NSD/mDNS autodiscovery → _elitecast._tcp
 *
 *  Thread-safe : tous les serveurs tournent hors du Main Thread.
 *  Wise Design – Prophète Josias – contact@wisedesign.gq
 * ═══════════════════════════════════════════════════════════════
 */
public class EliteNetworkManager {

    private static final String TAG = "EliteNetworkManager";
    public  static final int HTTP_PORT = 8080;
    public  static final int WS_PORT   = 8081;
    private static final String NSD_SERVICE_TYPE = "_elitecast._tcp.";
    private static final String NSD_SERVICE_NAME = "EliteCast-Regie";

    // ─── Singleton ────────────────────────────────────
    private static volatile EliteNetworkManager instance;
    public static EliteNetworkManager getInstance(Context ctx) {
        if (instance == null) synchronized (EliteNetworkManager.class) {
            if (instance == null) instance = new EliteNetworkManager(ctx.getApplicationContext());
        }
        return instance;
    }

    // ─── Champs ───────────────────────────────────────
    private final Context context;
    private final Gson gson = new GsonBuilder().create();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private EliteHttpServer  httpServer;
    private EliteWsServer    wsServer;
    private NsdManager       nsdManager;
    private NsdManager.RegistrationListener nsdRegistrationListener;
    private WifiManager.MulticastLock multicastLock;

    // Écrans connectés : screenId → WebSocket
    private final ConcurrentHashMap<String, WebSocket> connectedScreens = new ConcurrentHashMap<>();
    // Config par écran
    private final ConcurrentHashMap<String, ScreenConfig> screenConfigs = new ConcurrentHashMap<>();

    private NetworkListener listener;
    private boolean running = false;

    public interface NetworkListener {
        void onScreenConnected(ScreenConfig screen);
        void onScreenDisconnected(String screenId);
        void onCommandFromWeb(BroadcastPacket packet);
        void onServerReady(String httpUrl, String wsUrl);
        void onError(String message);
    }

    private EliteNetworkManager(Context ctx) {
        this.context = ctx;
    }

    // ═══════════════════════════════════════════════════
    //  DÉMARRAGE
    // ═══════════════════════════════════════════════════
    public void start(NetworkListener listener) {
        this.listener = listener;
        if (running) return;

        executor.submit(() -> {
            try {
                acquireMulticastLock();
                startHttpServer();
                startWebSocketServer();
                registerNsd();
                running = true;

                String ip = getLocalIpAddress();
                String httpUrl = "http://" + ip + ":" + HTTP_PORT;
                String wsUrl   = "ws://"   + ip + ":" + WS_PORT;
                Log.i(TAG, "EliteCast démarré → HTTP: " + httpUrl + " | WS: " + wsUrl);

                mainHandler.post(() -> {
                    if (this.listener != null)
                        this.listener.onServerReady(httpUrl, wsUrl);
                });
            } catch (Exception e) {
                Log.e(TAG, "Erreur démarrage: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    if (this.listener != null)
                        this.listener.onError("Erreur démarrage réseau: " + e.getMessage());
                });
            }
        });
    }

    public void stop() {
        running = false;
        try {
            if (httpServer != null) httpServer.stop();
            if (wsServer != null)   wsServer.stop();
            if (nsdManager != null && nsdRegistrationListener != null)
                nsdManager.unregisterService(nsdRegistrationListener);
            if (multicastLock != null && multicastLock.isHeld())
                multicastLock.release();
        } catch (Exception e) {
            Log.e(TAG, "Erreur arrêt: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════
    //  SERVEUR HTTP (NanoHTTPD) – PORT 8080
    // ═══════════════════════════════════════════════════
    private void startHttpServer() throws IOException {
        httpServer = new EliteHttpServer();
        httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        Log.i(TAG, "Serveur HTTP démarré sur le port " + HTTP_PORT);
    }

    private class EliteHttpServer extends NanoHTTPD {
        EliteHttpServer() { super(HTTP_PORT); }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            Log.d(TAG, "HTTP " + session.getMethod() + " " + uri);

            // ── API REST JSON ──────────────────────────
            if (uri.startsWith("/api/")) {
                return handleApiRequest(session, uri);
            }

            // ── Assets statiques (images, fonts, css) ──
            if (uri.startsWith("/assets/")) {
                return serveAsset(uri.substring(1));
            }

            // ── Interface Web principale ───────────────
            if (uri.equals("/") || uri.equals("/index.html")) {
                return serveWebInterface();
            }

            return newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "404 Not Found");
        }

        private Response handleApiRequest(IHTTPSession session, String uri) {
            try {
                if (uri.equals("/api/screens")) {
                    String json = gson.toJson(screenConfigs.values());
                    return newFixedLengthResponse(Status.OK, "application/json", json);
                }
                if (uri.equals("/api/broadcast") &&
                        session.getMethod() == Method.POST) {
                    Map<String, String> body = new ConcurrentHashMap<>();
                    session.parseBody(body);
                    String content = body.get("postData");
                    if (content != null) {
                        BroadcastPacket packet = gson.fromJson(content, BroadcastPacket.class);
                        broadcastToScreens(packet);
                        mainHandler.post(() -> {
                            if (listener != null) listener.onCommandFromWeb(packet);
                        });
                    }
                    return newFixedLengthResponse(Status.OK, "application/json",
                            "{\"status\":\"ok\"}");
                }
                if (uri.equals("/api/status")) {
                    String json = "{\"running\":true,\"screens\":" +
                            connectedScreens.size() + ",\"version\":\"1.0.0\"}";
                    return newFixedLengthResponse(Status.OK, "application/json", json);
                }
            } catch (Exception e) {
                Log.e(TAG, "API error: " + e.getMessage());
                return newFixedLengthResponse(Status.INTERNAL_ERROR,
                        "application/json", "{\"error\":\"" + e.getMessage() + "\"}");
            }
            return newFixedLengthResponse(Status.NOT_FOUND, "application/json",
                    "{\"error\":\"Not found\"}");
        }

        private Response serveWebInterface() {
            try {
                InputStream is = context.getAssets().open("web/index.html");
                byte[] bytes = is.readAllBytes();
                is.close();
                String html = new String(bytes, StandardCharsets.UTF_8);
                // Injection dynamique de l'IP locale
                html = html.replace("{{WS_HOST}}", getLocalIpAddress());
                html = html.replace("{{HTTP_HOST}}", getLocalIpAddress());
                return newFixedLengthResponse(Status.OK, "text/html", html);
            } catch (IOException e) {
                return newFixedLengthResponse(Status.INTERNAL_ERROR,
                        "text/html", "<h1>Erreur chargement interface</h1>");
            }
        }

        private Response serveAsset(String path) {
            try {
                InputStream is = context.getAssets().open(path);
                String mime = getMimeForPath(path);
                return newChunkedResponse(Status.OK, mime, is);
            } catch (IOException e) {
                return newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Asset not found");
            }
        }

        private String getMimeForPath(String path) {
            if (path.endsWith(".js"))   return "application/javascript";
            if (path.endsWith(".css"))  return "text/css";
            if (path.endsWith(".png"))  return "image/png";
            if (path.endsWith(".jpg"))  return "image/jpeg";
            if (path.endsWith(".svg"))  return "image/svg+xml";
            if (path.endsWith(".ttf"))  return "font/ttf";
            if (path.endsWith(".woff2"))return "font/woff2";
            return "application/octet-stream";
        }
    }

    // ═══════════════════════════════════════════════════
    //  SERVEUR WEBSOCKET – PORT 8081
    // ═══════════════════════════════════════════════════
    private void startWebSocketServer() throws InterruptedException {
        wsServer = new EliteWsServer(new InetSocketAddress(WS_PORT));
        wsServer.start();
        Log.i(TAG, "Serveur WebSocket démarré sur le port " + WS_PORT);
    }

    private class EliteWsServer extends WebSocketServer {
        EliteWsServer(InetSocketAddress address) {
            super(address);
            setReuseAddr(true);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            Log.i(TAG, "WS connexion entrante: " + conn.getRemoteSocketAddress());
            // Envoyer un ping pour récupérer l'ID de l'écran
            BroadcastPacket ping = new BroadcastPacket();
            ping.cmd = BroadcastPacket.CMD_PING;
            conn.send(gson.toJson(ping));
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            try {
                BroadcastPacket packet = gson.fromJson(message, BroadcastPacket.class);
                handleIncomingMessage(conn, packet);
            } catch (Exception e) {
                Log.e(TAG, "WS message parse error: " + e.getMessage());
            }
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            // Retrouver l'écran qui s'est déconnecté
            String removedId = null;
            for (Map.Entry<String, WebSocket> e : connectedScreens.entrySet()) {
                if (e.getValue().equals(conn)) {
                    removedId = e.getKey();
                    break;
                }
            }
            if (removedId != null) {
                connectedScreens.remove(removedId);
                screenConfigs.remove(removedId);
                final String finalId = removedId;
                Log.i(TAG, "Écran déconnecté: " + finalId);
                mainHandler.post(() -> {
                    if (listener != null) listener.onScreenDisconnected(finalId);
                });
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            Log.e(TAG, "WS error: " + ex.getMessage());
        }

        @Override
        public void onStart() {
            Log.i(TAG, "WS Server started");
            setConnectionLostTimeout(30);
        }
    }

    private void handleIncomingMessage(WebSocket conn, BroadcastPacket packet) {
        if (BroadcastPacket.CMD_PING.equals(packet.cmd) && packet.screenConfig != null) {
            // L'écran s'identifie
            ScreenConfig config = packet.screenConfig;
            if (config.screenId == null) config.screenId = UUID.randomUUID().toString();
            config.connected = true;
            config.ipAddress = conn.getRemoteSocketAddress().getAddress().getHostAddress();

            connectedScreens.put(config.screenId, conn);
            screenConfigs.put(config.screenId, config);

            Log.i(TAG, "Écran enregistré: " + config.deviceName + " [" + config.screenId + "]");

            mainHandler.post(() -> {
                if (listener != null) listener.onScreenConnected(config);
            });

            // Confirmer la connexion avec la config actuelle
            BroadcastPacket ack = new BroadcastPacket();
            ack.cmd = BroadcastPacket.CMD_CONFIGURE;
            ack.screenConfig = config;
            conn.send(gson.toJson(ack));
        }
    }

    // ═══════════════════════════════════════════════════
    //  DIFFUSION
    // ═══════════════════════════════════════════════════
    public void broadcastToScreens(BroadcastPacket packet) {
        String json = gson.toJson(packet);
        executor.submit(() -> {
            if (packet.targetScreenId != null) {
                // Ciblage d'un écran spécifique
                WebSocket ws = connectedScreens.get(packet.targetScreenId);
                if (ws != null && ws.isOpen()) {
                    ws.send(json);
                }
            } else {
                // Broadcast à tous les écrans connectés
                Collection<WebSocket> clients = connectedScreens.values();
                for (WebSocket ws : clients) {
                    if (ws.isOpen()) ws.send(json);
                }
            }
        });
    }

    public void configureScreen(String screenId, ScreenConfig config) {
        screenConfigs.put(screenId, config);
        BroadcastPacket packet = new BroadcastPacket();
        packet.cmd = BroadcastPacket.CMD_CONFIGURE;
        packet.targetScreenId = screenId;
        packet.screenConfig = config;
        broadcastToScreens(packet);
    }

    // ═══════════════════════════════════════════════════
    //  NSD – AUTODISCOVERY mDNS
    // ═══════════════════════════════════════════════════
    private void registerNsd() {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(NSD_SERVICE_NAME);
        serviceInfo.setServiceType(NSD_SERVICE_TYPE);
        serviceInfo.setPort(WS_PORT);

        // Métadonnées supplémentaires
        serviceInfo.setAttribute("httpPort", String.valueOf(HTTP_PORT));
        serviceInfo.setAttribute("version", "1.0");
        serviceInfo.setAttribute("app", "EliteCast");

        nsdRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo info, int errorCode) {
                Log.e(TAG, "NSD registration failed: " + errorCode);
            }
            @Override
            public void onUnregistrationFailed(NsdServiceInfo info, int errorCode) {
                Log.e(TAG, "NSD unregistration failed: " + errorCode);
            }
            @Override
            public void onServiceRegistered(NsdServiceInfo info) {
                Log.i(TAG, "NSD service registered: " + info.getServiceName());
            }
            @Override
            public void onServiceUnregistered(NsdServiceInfo info) {
                Log.i(TAG, "NSD service unregistered");
            }
        };

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD,
                nsdRegistrationListener);
    }

    // ═══════════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════════
    private void acquireMulticastLock() {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("EliteCastMulticast");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();
    }

    public String getLocalIpAddress() {
        try {
            WifiManager wifi = (WifiManager)
                    context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ip = wifi.getConnectionInfo().getIpAddress();
            return ((ip & 0xFF) + "." + ((ip >> 8) & 0xFF) +
                    "."  + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF));
        } catch (Exception e) {
            return "192.168.1.1";
        }
    }

    public Map<String, ScreenConfig> getScreenConfigs() {
        return screenConfigs;
    }

    public boolean isRunning() {
        return running;
    }
}
