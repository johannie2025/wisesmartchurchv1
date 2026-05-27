package com.wisedesign.wisesmartchurch.ui.control;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.wisedesign.wisesmartchurch.data.model.ScreenConfig;
import java.util.List;

public class ScreensAdapter extends RecyclerView.Adapter<ScreensAdapter.ViewHolder> {

    public interface OnScreenClick   { void onSelect(ScreenConfig screen); }
    public interface OnConfigChanged { void onChanged(ScreenConfig config); }

    private final List<ScreenConfig> screens;
    private final OnScreenClick   clickListener;
    private final OnConfigChanged configListener;

    public ScreensAdapter(List<ScreenConfig> screens,
                          OnScreenClick click,
                          OnConfigChanged config) {
        this.screens        = screens;
        this.clickListener  = click;
        this.configListener = config;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        ScreenConfig s = screens.get(pos);
        h.name.setText(s.deviceName != null ? s.deviceName : "Écran " + (pos + 1));
        h.role.setText(s.role != null ? s.role : ScreenConfig.ROLE_MAIN);
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onSelect(s);
        });
    }

    @Override public int getItemCount() { return screens.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, role;
        ViewHolder(View v) {
            super(v);
            name = v.findViewById(android.R.id.text1);
            role = v.findViewById(android.R.id.text2);
        }
    }
}
