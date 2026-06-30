package com.example.slagalica.regions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;

import java.util.ArrayList;
import java.util.List;

public class RegionAdapter extends RecyclerView.Adapter<RegionAdapter.RegionViewHolder> {

    private final List<RegionRankingItem> regions = new ArrayList<>();

    public void setRegions(List<RegionRankingItem> newRegions) {
        regions.clear();
        if (newRegions != null) {
            regions.addAll(newRegions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RegionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_region, parent, false);
        return new RegionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RegionViewHolder holder, int position) {
        RegionRankingItem item = regions.get(position);

        holder.tvRegionRank.setText(getRankText(position));
        holder.tvRegionIcon.setText(item.icon);
        holder.tvRegionName.setText(item.displayName);
        holder.tvRegionStars.setText("⭐ " + item.monthlyStars);
        holder.tvRegionStats.setText(
                "🟢 Active: " + item.activePlayers +
                        "    👥 Registered: " + item.totalPlayers
        );

        if (item.isMyRegion) {
            holder.itemView.setBackgroundResource(R.drawable.bg_region_my_card);
            holder.tvRegionName.setText("📍 " + item.displayName);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_profile_card);
            holder.tvRegionName.setText(item.displayName);
        }

        holder.itemView.setOnClickListener(v -> {
            String message =
                    "Monthly stars: ⭐ " + item.monthlyStars + "\n\n" +
                            "🥇 First places: " + item.firstPlaces + "\n" +
                            "🥈 Second places: " + item.secondPlaces + "\n" +
                            "🥉 Third places: " + item.thirdPlaces + "\n\n" +
                            "🟢 Active players: " + item.activePlayers + "\n" +
                            "👥 Registered players: " + item.totalPlayers;

            new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                    .setTitle(item.icon + " " + item.displayName + " statistics")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return regions.size();
    }

    private String getRankText(int position) {
        if (position == 0) return "🥇";
        if (position == 1) return "🥈";
        if (position == 2) return "🥉";
        return (position + 1) + ".";
    }

    static class RegionViewHolder extends RecyclerView.ViewHolder {
        TextView tvRegionRank;
        TextView tvRegionIcon;
        TextView tvRegionName;
        TextView tvRegionStars;
        TextView tvRegionStats;

        public RegionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRegionRank = itemView.findViewById(R.id.tvRegionRank);
            tvRegionIcon = itemView.findViewById(R.id.tvRegionIcon);
            tvRegionName = itemView.findViewById(R.id.tvRegionName);
            tvRegionStars = itemView.findViewById(R.id.tvRegionStars);
            tvRegionStats = itemView.findViewById(R.id.tvRegionStats);
        }
    }
}