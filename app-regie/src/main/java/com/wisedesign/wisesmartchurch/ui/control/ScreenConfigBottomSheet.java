package com.wisedesign.wisesmartchurch.ui.control;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.wisedesign.wisesmartchurch.data.model.ScreenConfig;

public class ScreenConfigBottomSheet extends BottomSheetDialogFragment {

    public interface ConfigListener { void onConfigChanged(ScreenConfig config); }

    private static final String ARG_SCREEN = "screen_json";
    private ConfigListener listener;
    private ScreenConfig screenConfig;

    public static ScreenConfigBottomSheet newInstance(ScreenConfig config) {
        ScreenConfigBottomSheet sheet = new ScreenConfigBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SCREEN, new com.google.gson.Gson().toJson(config));
        sheet.setArguments(args);
        return sheet;
    }

    public void setListener(ConfigListener l) { this.listener = l; }

    @Override
    public void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        if (getArguments() != null) {
            String json = getArguments().getString(ARG_SCREEN);
            screenConfig = new com.google.gson.Gson().fromJson(json, ScreenConfig.class);
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle saved) {
        // Layout minimal inline
        android.widget.LinearLayout root = new android.widget.LinearLayout(requireContext());
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(requireContext());
        title.setText(screenConfig != null ? "Config : " + screenConfig.deviceName : "Config écran");
        title.setTextSize(18f);
        root.addView(title);

        Button btnApply = new Button(requireContext());
        btnApply.setText("Appliquer");
        btnApply.setOnClickListener(v -> {
            if (listener != null && screenConfig != null) listener.onConfigChanged(screenConfig);
            dismiss();
        });
        root.addView(btnApply);

        return root;
    }
}
