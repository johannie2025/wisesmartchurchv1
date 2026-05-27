package com.wisedesign.elitecastpro.data.repository;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wisedesign.elitecastpro.data.model.BibleVerse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * ═══════════════════════════════════════════════════════════════
 *  BibleDownloadManager
 *
 *  Télécharge et stocke localement les versions bibliques JSON.
 *  Versions supportées :
 *   FR : Louis Segond 1910 (LSG), Nouvelle Bible Segond (NBS)
 *   ES : Reina-Valera 1960 (RVR60), Nueva Versión Internacional (NVI)
 *   EN : King James Version (KJV), English Standard Version (ESV)
 *
 *  Sources : bible-api.com (requiert internet pour le 1er téléchargement)
 *  Tout est stocké localement → fonctionnel 100% offline ensuite.
 * ═══════════════════════════════════════════════════════════════
 */
public class BibleDownloadManager {

    private static final String TAG = "BibleDownload";

    // Versions disponibles au téléchargement
    public enum BibleVersion {
        LSG("lsg", "fr", "Louis Segond (FR)",
                "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/fr_lsg.json"),
        NBS("nbs", "fr", "Nouvelle Bible Segond (FR)",
                "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/fr_nbs.json"),
        RVR60("rvr60", "es", "Reina-Valera 1960 (ES)",
                "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/es_rvr.json"),
        KJV("kjv", "en", "King James Version (EN)",
                "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/en_kjv.json");

        public final String code;
        public final String lang;
        public final String displayName;
        public final String url;

        BibleVersion(String code, String lang, String displayName, String url) {
            this.code = code;
            this.lang = lang;
            this.displayName = displayName;
            this.url = url;
        }
    }

    public interface DownloadCallback {
        void onProgress(int percent, String message);
        void onComplete(BibleVersion version);
        void onError(BibleVersion version, String error);
    }

    public interface SearchCallback {
        void onResults(List<BibleVerse> verses);
    }

    private final Context context;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;

    public BibleDownloadManager(Context context) {
        this.context    = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.gson       = new Gson();
        this.executor   = Executors.newSingleThreadExecutor();
    }

    // ═══════════════════════════════════════════════
    //  TÉLÉCHARGEMENT
    // ═══════════════════════════════════════════════
    public void download(BibleVersion version, DownloadCallback callback) {
        executor.submit(() -> {
            try {
                callback.onProgress(5, "Connexion au serveur...");
                Request request = new Request.Builder()
                        .url(version.url)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(version, "Réponse HTTP: " + response.code());
                        return;
                    }

                    callback.onProgress(30, "Téléchargement en cours...");
                    byte[] data = response.body().bytes();

                    callback.onProgress(70, "Sauvegarde locale...");
                    File bibleFile = getBibleFile(version.code);
                    try (FileOutputStream fos = new FileOutputStream(bibleFile)) {
                        fos.write(data);
                    }

                    callback.onProgress(100, "Bible " + version.displayName + " installée !");
                    callback.onComplete(version);
                    Log.i(TAG, "Bible téléchargée: " + version.displayName +
                            " (" + data.length / 1024 + " Ko)");
                }
            } catch (IOException e) {
                Log.e(TAG, "Erreur download: " + e.getMessage());
                callback.onError(version, "Erreur réseau: " + e.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════
    //  LECTURE LOCALE
    // ═══════════════════════════════════════════════
    public boolean isInstalled(BibleVersion version) {
        return getBibleFile(version.code).exists();
    }

    public List<BibleVerse> loadBible(BibleVersion version) {
        File file = getBibleFile(version.code);
        if (!file.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<BibleVerse>>(){}.getType();
            return gson.fromJson(reader, listType);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lecture bible: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public BibleVerse getVerse(BibleVersion version, String book, int chapter, int verse) {
        List<BibleVerse> all = loadBible(version);
        for (BibleVerse v : all) {
            if (v.book.equalsIgnoreCase(book) &&
                    v.chapter == chapter &&
                    v.verse   == verse) {
                return v;
            }
        }
        return null;
    }

    public void searchVerses(BibleVersion version, String query, SearchCallback callback) {
        executor.submit(() -> {
            List<BibleVerse> all     = loadBible(version);
            List<BibleVerse> results = new ArrayList<>();
            String lower = query.toLowerCase().trim();
            for (BibleVerse v : all) {
                if (v.text != null && v.text.toLowerCase().contains(lower)) {
                    results.add(v);
                    if (results.size() >= 50) break; // Limiter à 50 résultats
                }
            }
            callback.onResults(results);
        });
    }

    // ═══════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════
    private File getBibleFile(String code) {
        File dir = new File(context.getFilesDir(), "bibles");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "bible_" + code + ".json");
    }

    public long getBibleSizeKb(BibleVersion version) {
        File f = getBibleFile(version.code);
        return f.exists() ? f.length() / 1024 : 0;
    }

    public void deleteAll() {
        File dir = new File(context.getFilesDir(), "bibles");
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) f.delete();
    }
}
