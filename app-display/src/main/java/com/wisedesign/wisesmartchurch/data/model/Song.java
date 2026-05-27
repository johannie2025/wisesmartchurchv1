package com.wisedesign.wisesmartchurch.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class Song {
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
