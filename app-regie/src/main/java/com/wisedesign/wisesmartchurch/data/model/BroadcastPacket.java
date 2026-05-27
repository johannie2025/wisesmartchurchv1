package com.wisedesign.wisesmartchurch.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class BroadcastPacket {
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
