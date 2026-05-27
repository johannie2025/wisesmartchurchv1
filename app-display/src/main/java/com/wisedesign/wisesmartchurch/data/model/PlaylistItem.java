package com.wisedesign.wisesmartchurch.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class PlaylistItem {
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
