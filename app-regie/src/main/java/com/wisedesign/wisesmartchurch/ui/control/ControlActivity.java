package com.wisedesign.wisesmartchurch.ui.control;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.wisedesign.wisesmartchurch.regie.R;
import com.wisedesign.wisesmartchurch.data.model.BroadcastPacket;
import com.wisedesign.wisesmartchurch.data.model.ScreenConfig;
import com.wisedesign.wisesmartchurch.network.EliteNetworkManager;
import com.wisedesign.wisesmartchurch.service.EliteNetworkService;

import com.wisedesign.wisesmartchurch.util.LocaleHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 *  ControlActivity – Régie principale EliteCast Pro
 *
 *  Interface maître trilingue (FR/EN/ES) pour :
 *  • Diffusion de versets, chants, médias
 *  • Gestion multi-écrans avec prévisualisation
 *  • Configuration par écran
 *  • Démarrage du serveur réseau
 * ═══════════════════════════════════════════════════════════════
 */
public class ControlActivity extends AppCompatActivity
        implements EliteNetworkManager.NetworkListener {

    private static final String TAG = "ControlActivity";

    // ─── Vues ───────────────────────────────────────
    private TextView        tvServerUrl;
    private TextView        tvScreenCount;
    private View            serverStatusDot;
    private RecyclerView    rvScreens;
    private RecyclerView    rvPlaylist;
    private LinearLayout    panelBible;
    private LinearLayout    panelSongs;
    private LinearLayout    panelMedia;
    private LinearLayout    panelScreens;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabBroadcast;
    private LinearLayout    previewContainer;

    // ─── Data ────────────────────────────────────────
    private EliteNetworkManager networkManager;
    private ScreensAdapter  screensAdapter;
    private List<ScreenConfig> screens = new ArrayList<>();

    private String currentVerseText = "";
    private String currentVerseRef  = "";

    // ═══════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleHelper.applyLocale(this);
        setContentView(R.layout.activity_control);

        bindViews();
        setupNavigation();
        setupScreensList();
        startNetworkService();
        showPanel(R.id.panel_bible);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkManager != null) networkManager.stop();
    }

    // ═══════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════
    private void bindViews() {
        tvServerUrl    = findViewById(R.id.tv_server_url);
        tvScreenCount  = findViewById(R.id.tv_screen_count);
        serverStatusDot= findViewById(R.id.server_status_dot);
        rvScreens      = findViewById(R.id.rv_screens);
        bottomNav      = findViewById(R.id.bottom_navigation);
        fabBroadcast   = findViewById(R.id.fab_broadcast);
        panelBible     = findViewById(R.id.panel_bible);
        panelSongs     = findViewById(R.id.panel_songs);
        panelMedia     = findViewById(R.id.panel_media);
        panelScreens   = findViewById(R.id.panel_screens);
        previewContainer = findViewById(R.id.preview_container);

        fabBroadcast.setOnClickListener(v -> sendCurrentContent());
        findViewById(R.id.btn_clear_all).setOnClickListener(v -> clearAllScreens());
        findViewById(R.id.btn_open_display_mode).setOnClickListener(v -> openDisplayMode());
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_bible)   { showPanel(R.id.panel_bible);   return true; }
            if (id == R.id.nav_songs)   { showPanel(R.id.panel_songs);   return true; }
            if (id == R.id.nav_media)   { showPanel(R.id.panel_media);   return true; }
            if (id == R.id.nav_screens) { showPanel(R.id.panel_screens); return true; }
            return false;
        });
    }

    private void setupScreensList() {
        screensAdapter = new ScreensAdapter(screens, this::onScreenSelected,
                this::onScreenConfigChanged);
        rvScreens.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));
        rvScreens.setAdapter(screensAdapter);
    }

    // ═══════════════════════════════════════════════
    //  SERVICE RÉSEAU
    // ═══════════════════════════════════════════════
    private void startNetworkService() {
        Intent serviceIntent = new Intent(this, EliteNetworkService.class);
        startForegroundService(serviceIntent);

        networkManager = EliteNetworkManager.getInstance(this);
        networkManager.start(this);

        serverStatusDot.setBackgroundResource(R.drawable.dot_orange);
        tvServerUrl.setText(getString(R.string.starting_server));
    }

    // ═══════════════════════════════════════════════
    //  CALLBACKS RÉSEAU
    // ═══════════════════════════════════════════════
    @Override
    public void onServerReady(String httpUrl, String wsUrl) {
        serverStatusDot.setBackgroundResource(R.drawable.dot_green);
        tvServerUrl.setText("🌐 " + httpUrl + "\n🔌 " + wsUrl);
        Toast.makeText(this, getString(R.string.server_ready), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScreenConnected(ScreenConfig screen) {
        boolean exists = false;
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).screenId.equals(screen.screenId)) {
                screens.set(i, screen);
                screensAdapter.notifyItemChanged(i);
                exists = true;
                break;
            }
        }
        if (!exists) {
            screens.add(screen);
            screensAdapter.notifyItemInserted(screens.size() - 1);
        }
        updateScreenCount();
        Toast.makeText(this, "📺 " + screen.deviceName + " connecté", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScreenDisconnected(String screenId) {
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).screenId.equals(screenId)) {
                screens.remove(i);
                screensAdapter.notifyItemRemoved(i);
                break;
            }
        }
        updateScreenCount();
    }

    @Override
    public void onCommandFromWeb(BroadcastPacket packet) {
        // Synchroniser l'UI quand une commande vient de l'interface web
        if (BroadcastPacket.CMD_VERSE.equals(packet.cmd)) {
            updatePreview(packet.verseText, packet.verseReference);
        }
    }

    @Override
    public void onError(String message) {
        serverStatusDot.setBackgroundResource(R.drawable.dot_red);
        Toast.makeText(this, "❌ " + message, Toast.LENGTH_LONG).show();
    }

    // ═══════════════════════════════════════════════
    //  ACTIONS DE DIFFUSION
    // ═══════════════════════════════════════════════
    public void sendVerse(String text, String reference) {
        currentVerseText = text;
        currentVerseRef  = reference;
        updatePreview(text, reference);

        BroadcastPacket packet = BroadcastPacket.verse(reference, text);
        networkManager.broadcastToScreens(packet);
        Toast.makeText(this, "📤 " + reference, Toast.LENGTH_SHORT).show();
    }

    public void sendLowerThird(String title, String subtitle) {
        BroadcastPacket packet = BroadcastPacket.lowerThird(title, subtitle);
        networkManager.broadcastToScreens(packet);
    }

    public void sendMedia(String url, String type) {
        BroadcastPacket packet = BroadcastPacket.media(url, type);
        networkManager.broadcastToScreens(packet);
    }

    private void sendCurrentContent() {
        if (!currentVerseText.isEmpty()) {
            sendVerse(currentVerseText, currentVerseRef);
        }
    }

    private void clearAllScreens() {
        networkManager.broadcastToScreens(BroadcastPacket.clear());
        currentVerseText = "";
        currentVerseRef  = "";
        updatePreview("", "");
    }

    // ═══════════════════════════════════════════════
    //  GESTION ÉCRANS
    // ═══════════════════════════════════════════════
    private void onScreenSelected(ScreenConfig screen) {
        // Ouvrir le panneau de config de cet écran
        ScreenConfigBottomSheet sheet = ScreenConfigBottomSheet.newInstance(screen);
        sheet.setListener(config -> {
            networkManager.configureScreen(config.screenId, config);
        });
        sheet.show(getSupportFragmentManager(), "screen_config");
    }

    private void onScreenConfigChanged(ScreenConfig config) {
        networkManager.configureScreen(config.screenId, config);
    }

    private void openDisplayMode() {
        // Basculer vers l'application display (APK séparé)
        try {
            Intent intent = new Intent();
            intent.setClassName(
                "com.wisedesign.wisesmartchurch.display",
                "com.wisedesign.wisesmartchurch.ui.display.TvDisplayActivity"
            );
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this,
                "Application Display non installée", Toast.LENGTH_SHORT).show();
        }
    }

    // ═══════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════
    private void showPanel(int panelId) {
        panelBible.setVisibility(panelId   == R.id.panel_bible   ? View.VISIBLE : View.GONE);
        panelSongs.setVisibility(panelId   == R.id.panel_songs   ? View.VISIBLE : View.GONE);
        panelMedia.setVisibility(panelId   == R.id.panel_media   ? View.VISIBLE : View.GONE);
        panelScreens.setVisibility(panelId == R.id.panel_screens ? View.VISIBLE : View.GONE);
    }

    private void updateScreenCount() {
        tvScreenCount.setText(screens.size() + " " +
                getString(R.string.screens_connected));
    }

    private void updatePreview(String text, String reference) {
        TextView previewText = previewContainer.findViewWithTag("preview_text");
        TextView previewRef  = previewContainer.findViewWithTag("preview_ref");
        if (previewText != null) previewText.setText(text);
        if (previewRef  != null) previewRef.setText(reference);
    }
}
