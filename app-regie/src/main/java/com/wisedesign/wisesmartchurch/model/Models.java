package com.wisedesign.elitecastpro.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// ═══════════════════════════════════════════════════
//  MODÈLES DE DONNÉES ELITECAST PRO
//  Wise Design – Prophète Josias
// ═══════════════════════════════════════════════════

// ─── Configuration d'un écran connecté ───────────────
class ScreenConfig {
    public static final String ROLE_MAIN          = "MAIN";         // Versets complets
    public static final String ROLE_LOWER_THIRD   = "LOWER_THIRD"; // Incrustation bas
    public static final String ROLE_STAGE_RETURN  = "STAGE_RETURN";// Retour scène
    public static final String ROLE_AUDIENCE      = "AUDIENCE";    // Salle/Latéral
    public static final String ROLE_CHOIR         = "CHOIR";       // Chorale
    public static final String ROLE_PODIUM        = "PODIUM";      // Podium prédicateur

    @SerializedName("screenId")
    public String screenId;

    @SerializedName("deviceName")
    public String deviceName;

    @SerializedName("role")
    public String role = ROLE_MAIN;

    @SerializedName("ipAddress")
    public String ipAddress;

    @SerializedName("connected")
    public boolean connected;

    @SerializedName("showBackground")
    public boolean showBackground = true;

    @SerializedName("showText")
    public boolean showText = true;

    @SerializedName("showLowerThird")
    public boolean showLowerThird = false;

    @SerializedName("fontSize")
    public int fontSize = 48;

    @SerializedName("textColor")
    public String textColor = "#FFFFFF";

    @SerializedName("overlayOpacity")
    public float overlayOpacity = 0.5f;

    @SerializedName("transition")
    public String transition = "FADE"; // FADE | SLIDE_LEFT | SLIDE_UP | NONE

    public ScreenConfig() {}

    public ScreenConfig(String screenId, String deviceName, String ipAddress) {
        this.screenId = screenId;
        this.deviceName = deviceName;
        this.ipAddress = ipAddress;
        this.connected = true;
    }
}

// ─── Paquet de commande WebSocket ────────────────────
class BroadcastPacket {
    public static final String CMD_VERSE        = "VERSE";
    public static final String CMD_CLEAR        = "CLEAR";
    public static final String CMD_MEDIA        = "MEDIA";
    public static final String CMD_MEDIA_STOP   = "MEDIA_STOP";
    public static final String CMD_TRANSITION   = "TRANSITION";
    public static final String CMD_LOWER_THIRD  = "LOWER_THIRD";
    public static final String CMD_CONFIGURE    = "CONFIGURE";
    public static final String CMD_PING         = "PING";
    public static final String CMD_ANNOUNCE     = "ANNOUNCE";
    public static final String CMD_SONG_SLIDE   = "SONG_SLIDE";

    @SerializedName("cmd")
    public String cmd;

    @SerializedName("targetScreenId")
    public String targetScreenId; // null = broadcast all

    @SerializedName("verseReference")
    public String verseReference;

    @SerializedName("verseText")
    public String verseText;

    @SerializedName("lowerThirdTitle")
    public String lowerThirdTitle;

    @SerializedName("lowerThirdSubtitle")
    public String lowerThirdSubtitle;

    @SerializedName("mediaUrl")
    public String mediaUrl;

    @SerializedName("mediaType")
    public String mediaType; // VIDEO | IMAGE | RTSP | RTMP | SRT

    @SerializedName("transition")
    public String transition;

    @SerializedName("screenConfig")
    public ScreenConfig screenConfig;

    @SerializedName("timestamp")
    public long timestamp = System.currentTimeMillis();

    public static BroadcastPacket verse(String ref, String text) {
        BroadcastPacket p = new BroadcastPacket();
        p.cmd = CMD_VERSE;
        p.verseReference = ref;
        p.verseText = text;
        p.transition = "FADE";
        return p;
    }

    public static BroadcastPacket clear() {
        BroadcastPacket p = new BroadcastPacket();
        p.cmd = CMD_CLEAR;
        return p;
    }

    public static BroadcastPacket lowerThird(String title, String subtitle) {
        BroadcastPacket p = new BroadcastPacket();
        p.cmd = CMD_LOWER_THIRD;
        p.lowerThirdTitle = title;
        p.lowerThirdSubtitle = subtitle;
        return p;
    }

    public static BroadcastPacket media(String url, String type) {
        BroadcastPacket p = new BroadcastPacket();
        p.cmd = CMD_MEDIA;
        p.mediaUrl = url;
        p.mediaType = type;
        return p;
    }
}

// ─── Verset Biblique ─────────────────────────────────
class BibleVerse {
    @SerializedName("book")
    public String book;

    @SerializedName("chapter")
    public int chapter;

    @SerializedName("verse")
    public int verse;

    @SerializedName("text")
    public String text;

    @SerializedName("translation")
    public String translation; // LSG, NBS, RVR60, ESV

    @SerializedName("lang")
    public String lang; // fr, es, en

    public String getReference() {
        return book + " " + chapter + ":" + verse;
    }
}

// ─── Diapositive de chanson ───────────────────────────
class SongSlide {
    @SerializedName("slideIndex")
    public int slideIndex;

    @SerializedName("content")
    public String content;

    @SerializedName("type")
    public String type; // VERSE | CHORUS | BRIDGE | INTRO | OUTRO

    @SerializedName("repeatCount")
    public int repeatCount = 1;
}

// ─── Chanson ──────────────────────────────────────────
class Song {
    @SerializedName("id")
    public String id;

    @SerializedName("title")
    public String title;

    @SerializedName("artist")
    public String artist;

    @SerializedName("lang")
    public String lang;

    @SerializedName("slides")
    public List<SongSlide> slides;

    @SerializedName("linkedMediaUrl")
    public String linkedMediaUrl;
}

// ─── Élément de Playlist ─────────────────────────────
class PlaylistItem {
    public static final String TYPE_VERSE   = "VERSE";
    public static final String TYPE_SONG    = "SONG";
    public static final String TYPE_MEDIA   = "MEDIA";
    public static final String TYPE_ANNOUNCE= "ANNOUNCE";

    @SerializedName("itemId")
    public String itemId;

    @SerializedName("type")
    public String type;

    @SerializedName("title")
    public String title;

    @SerializedName("data")
    public String data; // JSON du contenu

    @SerializedName("orderIndex")
    public int orderIndex;

    @SerializedName("duration")
    public int duration; // en secondes (0 = manuel)
}

// ─── Événement / Service ────────────────────────────
class EventSession {
    @SerializedName("sessionId")
    public String sessionId;

    @SerializedName("title")
    public String title;

    @SerializedName("date")
    public String date;

    @SerializedName("items")
    public List<PlaylistItem> items;

    @SerializedName("activeItemIndex")
    public int activeItemIndex = 0;

    @SerializedName("screenConfigs")
    public List<ScreenConfig> screenConfigs;
}
