package com.wisedesign.wisesmartchurch.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class SongSlide {
    @SerializedName("slideIndex")
    public int slideIndex;

    @SerializedName("content")
    public String content;

    @SerializedName("type")
    public String type; // VERSE | CHORUS | BRIDGE | INTRO | OUTRO

    @SerializedName("repeatCount")
    public int repeatCount = 1;
}
