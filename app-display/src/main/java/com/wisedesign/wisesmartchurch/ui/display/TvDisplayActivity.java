package com.wisedesign.wisesmartchurch.ui.display;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.gson.Gson;
import com.wisedesign.wisesmartchurch.R;
import com.wisedesign.wisesmartchurch.data.model.BroadcastPacket;
import com.wisedesign.wisesmartchurch.data.model.ScreenConfig;
import com.wisedesign.wisesmartchurch.network.NsdDiscoveryManager;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.UUID;

/**
 * ═══════════════════════════════════════════════════════════════
 *  TvDisplayActivity – Interface de l'écran d'affichage (TV Box)
 *
 *  Architecture par couches (FrameLayout) :
 *  ┌────────────────────────────────────────┐
 *  │  Layer 0 – Fond vidéo / ExoPlayer      │
 *  │  Layer 1 – Overlay / Lower-Third       │
 *  │  Layer 2 – Texte / Versets / Lyrics    │
 *  └────────────────────────────────────────┘
 *
 *  Connexion WebSocket auto via NSD mDNS
 *  Wise Design – Prophète Josias
 * ═══════════════════════════════════════════════════════════════
 */
@OptIn(markerClass = UnstableApi.class)
public class TvDisplayActivity extends Activity {

    private static final String TAG       = "TvDisplay";
    private static final long   RECONNECT_DELAY_MS = 3000;

    // ─── Vues ────────────────────────────────────────
    private FrameLayout        rootFrame;
    private PlayerView         playerView;           // Layer 0
    private View               overlayLayer;         // Layer 1
    private LinearLayout       lowerThirdContainer;  // Layer 1 (bas)
    private TextView           lowerThirdTitle;
    private TextView           lowerThirdSubtitle;
    private TextView           verseTextView;        // Layer 2
    private TextView           verseReferenceView;   // Layer 2
    private TextView           statusView;           // Debug/Status
    private View               connectionDot;

    // ─── Moteur ──────────────────────────────────────
    private ExoPlayer          exoPlayer;
    private WebSocketClient    wsClient;
    private NsdDiscoveryManager nsdDiscovery;
    private Handler            mainHandler   = new Handler(Looper.getMainLooper());
    private Handler            reconnectHandler = new Handler(Looper.getMainLooper());
    private Gson               gson          = new Gson();

    // ─── Config ──────────────────────────────────────
    private String  screenId;
    private String  deviceName;
    private ScreenConfig currentConfig;
    private String  masterHost;
    private int     masterWsPort;
    private boolean isConnected = false;

    // ═══════════════════════════════════════════════════
    //  LIFECYCLE
    // ═══════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullscreen();
        setContentView(R.layout.activity_tv_display);

        screenId   = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        deviceName = android.os.Build.MODEL + " [" + screenId + "]";

        bindViews();
        initExoPlayer();
        showStatus("🔍 Recherche de la régie sur le réseau...");
        startNsdDiscovery();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exoPlayer != null) exoPlayer.play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) exoPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectWs();
        releaseExoPlayer();
        if (nsdDiscovery != null) nsdDiscovery.stopDiscovery();
    }

    // ═══════════════════════════════════════════════════
    //  INITIALISATION VUES
    // ═══════════════════════════════════════════════════
    private void bindViews() {
        rootFrame           = findViewById(R.id.root_frame);
        playerView          = findViewById(R.id.player_view);
        overlayLayer        = findViewById(R.id.overlay_layer);
        lowerThirdContainer = findViewById(R.id.lower_third_container);
        lowerThirdTitle     = findViewById(R.id.lower_third_title);
        lowerThirdSubtitle  = findViewById(R.id.lower_third_subtitle);
        verseTextView       = findViewById(R.id.verse_text);
        verseReferenceView  = findViewById(R.id.verse_reference);
        statusView          = findViewById(R.id.status_text);
        connectionDot       = findViewById(R.id.connection_dot);

        lowerThirdContainer.setVisibility(View.GONE);
    }

    // ═══════════════════════════════════════════════════
    //  EXOPLAYER – Layer 0 Fond Vidéo
    // ═══════════════════════════════════════════════════
    private void initExoPlayer() {
        exoPlayer = new ExoPlayer.Builder(this)
                .setHandleAudioBecomingNoisy(true)
                .build();

        playerView.setPlayer(exoPlayer);
        playerView.setUseController(false);
        playerView.setKeepScreenOn(true);

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    // Boucle automatique pour les vidéos de fond
                    exoPlayer.seekTo(0);
                    exoPlayer.play();
                }
            }
        });
    }

    private void loadMedia(String url, String type) {
        MediaItem mediaItem = MediaItem.fromUri(url);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        exoPlayer.play();
        animateFadeIn(playerView);
    }

    private void stopMedia() {
        animateFadeOut(playerView, () -> {
            exoPlayer.stop();
            exoPlayer.clearMediaItems();
        });
    }

    private void releaseExoPlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    // ═══════════════════════════════════════════════════
    //  NSD DISCOVERY
    // ═══════════════════════════════════════════════════
    private void startNsdDiscovery() {
        nsdDiscovery = new NsdDiscoveryManager(this);
        nsdDiscovery.discover(new NsdDiscoveryManager.DiscoveryCallback() {
            @Override
            public void onMasterFound(String host, int wsPort, int httpPort) {
                masterHost   = host;
                masterWsPort = wsPort;
                showStatus("✅ Régie trouvée → " + host + ":" + wsPort);
                mainHandler.postDelayed(() -> connectWebSocket(host, wsPort), 500);
            }

            @Override
            public void onDiscoveryFailed(int errorCode) {
                showStatus("⚠️ Découverte NSD échouée (" + errorCode + ")\n" +
                        "Vérifiez le WiFi ou saisissez l'IP manuellement");
            }
        });
    }

    // ═══════════════════════════════════════════════════
    //  WEBSOCKET CLIENT
    // ═══════════════════════════════════════════════════
    private void connectWebSocket(String host, int port) {
        String wsUrl = "ws://" + host + ":" + port;
        Log.i(TAG, "Connexion WebSocket → " + wsUrl);
        showStatus("🔗 Connexion à la régie...");

        try {
            wsClient = new WebSocketClient(URI.create(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    isConnected = true;
                    Log.i(TAG, "WS connecté !");
                    sendIdentification();
                    mainHandler.post(() -> {
                        showStatus("");
                        setConnectionIndicator(true);
                    });
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "WS message reçu: " + message.substring(0,
                            Math.min(100, message.length())));
                    try {
                        BroadcastPacket packet = gson.fromJson(message, BroadcastPacket.class);
                        mainHandler.post(() -> handlePacket(packet));
                    } catch (Exception e) {
                        Log.e(TAG, "Parse error: " + e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected = false;
                    Log.w(TAG, "WS fermé: " + reason);
                    mainHandler.post(() -> {
                        setConnectionIndicator(false);
                        showStatus("🔴 Connexion perdue. Reconnexion...");
                        scheduleReconnect();
                    });
                }

                @Override
                public void onError(Exception ex) {
                    isConnected = false;
                    Log.e(TAG, "WS erreur: " + ex.getMessage());
                    mainHandler.post(() -> {
                        setConnectionIndicator(false);
                        showStatus("❌ Erreur: " + ex.getMessage());
                        scheduleReconnect();
                    });
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "Connexion WS impossible: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private void sendIdentification() {
        ScreenConfig config = new ScreenConfig(screenId, deviceName,
                masterHost != null ? masterHost : "unknown");
        config.role = ScreenConfig.ROLE_MAIN;

        BroadcastPacket ping = new BroadcastPacket();
        ping.cmd = BroadcastPacket.CMD_PING;
        ping.screenConfig = config;

        wsClient.send(gson.toJson(ping));
    }

    private void disconnectWs() {
        try {
            if (wsClient != null) wsClient.closeBlocking();
        } catch (Exception e) {
            Log.e(TAG, "Erreur déconnexion WS: " + e.getMessage());
        }
    }

    private void scheduleReconnect() {
        reconnectHandler.postDelayed(() -> {
            if (masterHost != null) {
                connectWebSocket(masterHost, masterWsPort);
            } else {
                startNsdDiscovery();
            }
        }, RECONNECT_DELAY_MS);
    }

    // ═══════════════════════════════════════════════════
    //  GESTION DES COMMANDES REÇUES
    // ═══════════════════════════════════════════════════
    private void handlePacket(BroadcastPacket packet) {
        // Vérifier si ce paquet nous est destiné
        if (packet.targetScreenId != null &&
                !packet.targetScreenId.equals(screenId)) return;

        switch (packet.cmd) {
            case BroadcastPacket.CMD_VERSE:
                showVerse(packet.verseText, packet.verseReference, packet.transition);
                break;

            case BroadcastPacket.CMD_CLEAR:
                clearDisplay(packet.transition);
                break;

            case BroadcastPacket.CMD_MEDIA:
                loadMedia(packet.mediaUrl, packet.mediaType);
                break;

            case BroadcastPacket.CMD_MEDIA_STOP:
                stopMedia();
                break;

            case BroadcastPacket.CMD_LOWER_THIRD:
                showLowerThird(packet.lowerThirdTitle, packet.lowerThirdSubtitle);
                break;

            case BroadcastPacket.CMD_CONFIGURE:
                applyConfig(packet.screenConfig);
                break;

            case BroadcastPacket.CMD_PING:
                // Répondre au ping
                sendIdentification();
                break;

            case BroadcastPacket.CMD_SONG_SLIDE:
                showVerse(packet.verseText, packet.verseReference, "FADE");
                break;

            case BroadcastPacket.CMD_ANNOUNCE:
                showAnnounce(packet.verseText);
                break;
        }
    }

    // ═══════════════════════════════════════════════════
    //  ACTIONS D'AFFICHAGE
    // ═══════════════════════════════════════════════════
    private void showVerse(String text, String reference, String transition) {
        applyTransitionOut(verseTextView, transition, () -> {
            verseTextView.setText(text != null ? text : "");
            verseReferenceView.setText(reference != null ? reference : "");
            applyTransitionIn(verseTextView, transition);
        });
    }

    private void clearDisplay(String transition) {
        applyTransitionOut(verseTextView, transition, () -> {
            verseTextView.setText("");
            verseReferenceView.setText("");
        });
        hideLowerThird();
    }

    private void showLowerThird(String title, String subtitle) {
        lowerThirdTitle.setText(title != null ? title : "");
        lowerThirdSubtitle.setText(subtitle != null ? subtitle : "");
        lowerThirdContainer.setVisibility(View.VISIBLE);
        animateFadeIn(lowerThirdContainer);
    }

    private void hideLowerThird() {
        animateFadeOut(lowerThirdContainer, () ->
                lowerThirdContainer.setVisibility(View.GONE));
    }

    private void showAnnounce(String text) {
        verseTextView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,
                currentConfig != null ? currentConfig.fontSize + 8 : 56);
        showVerse(text, "", "FADE");
    }

    private void applyConfig(ScreenConfig config) {
        if (config == null) return;
        currentConfig = config;

        if (config.fontSize > 0) {
            verseTextView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,
                    config.fontSize);
        }
        try {
            int color = android.graphics.Color.parseColor(
                    config.textColor != null ? config.textColor : "#FFFFFF");
            verseTextView.setTextColor(color);
            verseReferenceView.setTextColor(color);
        } catch (Exception ignored) {}

        overlayLayer.setAlpha(config.overlayOpacity);
        overlayLayer.setVisibility(config.showBackground ? View.VISIBLE : View.GONE);
        verseTextView.setVisibility(config.showText ? View.VISIBLE : View.GONE);
    }

    // ═══════════════════════════════════════════════════
    //  TRANSITIONS MATÉRIELLES
    // ═══════════════════════════════════════════════════
    private void applyTransitionIn(View view, String transition) {
        if (transition == null) transition = "FADE";
        switch (transition) {
            case "FADE":
                animateFadeIn(view);
                break;
            case "SLIDE_LEFT":
                view.setTranslationX(view.getWidth());
                view.animate().translationX(0)
                        .setDuration(300)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                break;
            case "SLIDE_UP":
                view.setTranslationY(view.getHeight());
                view.animate().translationY(0)
                        .setDuration(300)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                break;
            default:
                view.setAlpha(1f);
                break;
        }
    }

    private void applyTransitionOut(View view, String transition, Runnable onComplete) {
        if (transition == null || transition.equals("NONE")) {
            view.setAlpha(0f);
            onComplete.run();
            view.setAlpha(1f);
            return;
        }
        animateFadeOut(view, () -> {
            onComplete.run();
            view.setAlpha(0f);
        });
    }

    private void animateFadeIn(View view) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        anim.setDuration(400);
        anim.start();
    }

    private void animateFadeOut(View view, Runnable onEnd) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        anim.setDuration(300);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onEnd != null) onEnd.run();
                view.setAlpha(1f);
            }
        });
        anim.start();
    }

    // ═══════════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════════
    private void showStatus(String msg) {
        if (statusView != null) {
            statusView.setText(msg);
            statusView.setVisibility(msg.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void setConnectionIndicator(boolean connected) {
        if (connectionDot != null) {
            connectionDot.setBackgroundResource(connected ?
                    R.drawable.dot_green : R.drawable.dot_red);
        }
    }

    private void setFullscreen() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
