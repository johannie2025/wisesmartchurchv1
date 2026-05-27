package com.wisedesign.wisesmartchurch.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;


public class ScreenConfig {
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
