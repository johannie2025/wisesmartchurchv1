package com.wisedesign.wisesmartchurch.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class EventSession {
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
