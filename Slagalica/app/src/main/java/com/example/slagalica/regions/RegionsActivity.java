package com.example.slagalica.regions;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.MapView;

import java.util.List;

public class RegionsActivity extends AppCompatActivity {

    private TextView btnBackRegions;
    private TextView tvMyRegion;
    private TextView tvMyRegionStars;
    private RecyclerView rvRegions;
    private MapView mapRegions;
    private View btnSimulateRegionCycle;

    private RegionAdapter adapter;
    private RegionRepository repository;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_regions);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        currentUid = user.getUid();

        initializeViews();
        setupRecyclerView();
        setupListeners();
        setupMap();
        //checkMonthlyRegionCycle();
        loadRegions();
        loadMapPoints();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapRegions != null) {
            mapRegions.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapRegions != null) {
            mapRegions.onPause();
        }
        super.onPause();
    }

    private void initializeViews() {
        btnBackRegions = findViewById(R.id.btnBackRegions);
        tvMyRegion = findViewById(R.id.tvMyRegion);
        tvMyRegionStars = findViewById(R.id.tvMyRegionStars);
        rvRegions = findViewById(R.id.rvRegions);
        mapRegions = findViewById(R.id.mapRegions);
        btnSimulateRegionCycle = findViewById(R.id.btnSimulateRegionCycle);

        repository = new RegionRepository();
    }

    private void setupRecyclerView() {
        adapter = new RegionAdapter();
        rvRegions.setLayoutManager(new LinearLayoutManager(this));
        rvRegions.setAdapter(adapter);
        rvRegions.setNestedScrollingEnabled(false);
        rvRegions.setHasFixedSize(false);
    }
    private void setupListeners() {
        btnBackRegions.setOnClickListener(v -> finish());

        btnSimulateRegionCycle.setOnClickListener(v -> {
            repository.simulateMonthlyRegionCycle(new RegionRepository.RegionCycleCallback() {
                @Override
                public void onSuccess(String message) {
                    new androidx.appcompat.app.AlertDialog.Builder(RegionsActivity.this)
                            .setTitle("🏆 Monthly regional cycle finished")
                            .setMessage(message + "\n\nMonthly stars have been reset.\nAvatar borders have been updated.")
                            .setPositiveButton("OK", null)
                            .show();

                    loadRegions();
                    loadMapPoints();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(RegionsActivity.this, "Failed to finish cycle", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadRegions() {
        repository.loadRegionalRanking(currentUid, new RegionRepository.RegionRankingCallback() {
            @Override
            public void onSuccess(List<RegionRankingItem> regions) {
                android.util.Log.d("REGIONS", "Loaded regions = " + regions.size());

                for (RegionRankingItem item : regions) {
                    android.util.Log.d(
                            "REGIONS",
                            item.regionId + " stars=" + item.monthlyStars
                    );
                }

                adapter.setRegions(regions);

                for (RegionRankingItem item : regions) {
                    if (item.isMyRegion) {
                        tvMyRegion.setText(item.icon + " My region: " + item.displayName);
                        tvMyRegionStars.setText("Monthly stars: " + item.monthlyStars);
                        break;
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(
                        RegionsActivity.this,
                        "Failed to load regions",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void setupMap() {
        if (mapRegions == null) return;

        mapRegions.setMultiTouchControls(true);
        mapRegions.getController().setZoom(7.0);
        mapRegions.getController().setCenter(
                new org.osmdroid.util.GeoPoint(44.0165, 21.0059)
        );
    }

    private void loadMapPoints() {
        repository.loadMapPoints(new RegionRepository.RegionMapCallback() {
            @Override
            public void onSuccess(List<RegionMapPoint> points) {
                if (mapRegions == null) return;

                mapRegions.getOverlays().clear();

                for (RegionMapPoint point : points) {
                    Marker marker = new Marker(mapRegions);

                    if (point.uid.equals(currentUid)) {
                        marker.setIcon(ContextCompat.getDrawable(RegionsActivity.this, R.drawable.ic_my_region_marker));
                    } else {
                        marker.setIcon(ContextCompat.getDrawable(RegionsActivity.this, R.drawable.ic_region_marker));
                    }
                    marker.setPosition(new GeoPoint(point.latitude, point.longitude));
                    marker.setTitle(point.username);
                    if (point.uid.equals(currentUid)) {
                        marker.setTitle("You • " + point.username);
                    }
                    marker.setSubDescription(RegionManager.getDisplayName(point.regionId));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                    mapRegions.getOverlays().add(marker);
                }

                mapRegions.invalidate();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(
                        RegionsActivity.this,
                        "Failed to load map points",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void checkMonthlyRegionCycle() {
        repository.checkAndProcessMonthlyRegionCycle(new RegionRepository.RegionCycleCallback() {
            @Override
            public void onSuccess(String message) {
                if (message != null && !message.isEmpty()) {
                    new androidx.appcompat.app.AlertDialog.Builder(RegionsActivity.this)
                            .setTitle("🏆 Monthly regional cycle finished")
                            .setMessage(message + "\n\nMonthly stars have been reset.\nAvatar borders have been updated.")
                            .setPositiveButton("OK", null)
                            .show();

                    loadRegions();
                    loadMapPoints();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(RegionsActivity.this, "Failed to check monthly cycle", Toast.LENGTH_SHORT).show();
            }
        });
    }
}