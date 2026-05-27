package com.wisedesign.wisesmartchurch.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class BibleVerse {
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
