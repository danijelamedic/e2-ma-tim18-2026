package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.data.PlayerProfileLoader;

import java.util.List;

public class ChallengeResultActivity extends AppCompatActivity {

    private LinearLayout scoresList;
    private long[] scores;
    private List<String> playerIds;
    private String currentUid;

    private final Handler animHandler = new Handler(Looper.getMainLooper());
    private boolean confettiActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_result);

        long myScore     = getIntent().getLongExtra("myScore", 0);
        String resultMsg = getIntent().getStringExtra("resultMsg");
        scores           = getIntent().getLongArrayExtra("playerScores");
        playerIds        = getIntent().getStringArrayListExtra("playerIds");
        currentUid        = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        TextView tvResult  = findViewById(R.id.tvChallengeResult);
        TextView tvMyScore = findViewById(R.id.tvChallengeMyScore);
        scoresList         = findViewById(R.id.scoresList);

        tvResult.setText(resultMsg);
        tvMyScore.setText("Your score: " + myScore + " pts");

        buildRankingRows();

        findViewById(R.id.btnBackHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        playEntranceAnimations();
    }

    /**
     * Builds one ranking row per player. If we have playerIds (uids, already
     * sorted by score by the caller), resolve each to a real username/avatar
     * via PlayerProfileLoader. Falls back to generic "Player N" rows if the
     * ids are missing for any reason (defensive — keeps old behaviour working).
     */
    private void buildRankingRows() {
        if (scores == null) return;

        for (int i = 0; i < scores.length; i++) {
            final int rank = i;
            View row = createRankRow(rank, scores[rank], null, -1);
            scoresList.addView(row);

            if (playerIds != null && rank < playerIds.size()) {
                String uid = playerIds.get(rank);
                Object tag = row.getTag();
                if (!(tag instanceof RowViews)) continue;
                RowViews rowViews = (RowViews) tag;
                PlayerProfileLoader.load(uid, summary -> {
                    boolean isMe = uid.equals(currentUid);
                    String label = summary.username != null ? summary.username : "Player";
                    rowViews.name.setText(isMe ? label + "  (You)" : label);
                    rowViews.avatar.setImageResource(summary.avatarResId);
                });
            }
        }
    }

    private View createRankRow(int rank, long score, String fallbackName, int avatarResId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(64));
        rowLp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowLp);
        row.setPadding(dp(12), dp(8), dp(14), dp(8));
        row.setBackgroundResource(backgroundForRank(rank));
        row.setElevation(dp(rank == 0 ? 4 : 2));

        // medal / position badge
        TextView tvMedal = new TextView(this);
        LinearLayout.LayoutParams medalLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        medalLp.setMarginEnd(dp(10));
        tvMedal.setLayoutParams(medalLp);
        tvMedal.setGravity(android.view.Gravity.CENTER);
        tvMedal.setTextSize(rank < 3 ? 20 : 14);
        tvMedal.setText(medalForRank(rank));
        if (rank >= 3) {
            tvMedal.setTextColor(0xFF5B2FC4);
            tvMedal.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        row.addView(tvMedal);

        // avatar
        ImageView ivAvatar = new ImageView(this);
        ivAvatar.setId(View.generateViewId());
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        avatarLp.setMarginEnd(dp(12));
        ivAvatar.setLayoutParams(avatarLp);
        ivAvatar.setBackgroundResource(R.drawable.bg_avatar_circle);
        ivAvatar.setPadding(dp(5), dp(5), dp(5), dp(5));
        ivAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        ivAvatar.setImageResource(avatarResId > 0 ? avatarResId : R.drawable.avatar_owl);
        row.addView(ivAvatar);

        // name (fills remaining space)
        TextView tvName = new TextView(this);
        tvName.setId(View.generateViewId());
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameLp);
        tvName.setText(fallbackName != null ? fallbackName : "Player " + (rank + 1));
        tvName.setTextSize(15);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setTextColor(rank == 0 ? 0xFF6B4A00 : 0xFF2D1B5E);
        row.addView(tvName);

        // score pill
        TextView tvScore = new TextView(this);
        tvScore.setText(score + " pts");
        tvScore.setTextSize(14);
        tvScore.setTypeface(null, android.graphics.Typeface.BOLD);
        tvScore.setTextColor(rank == 0 ? 0xFF6B4A00 : 0xFF5B2FC4);
        row.addView(tvScore);

        // stash references so buildRankingRows() can update them once the
        // async profile lookup resolves, without relying on static R.id's
        // that don't exist for dynamically created views.
        row.setTag(new RowViews(ivAvatar, tvName));

        return row;
    }

    /** Simple holder for the two views inside a row that get updated asynchronously. */
    private static class RowViews {
        final ImageView avatar;
        final TextView name;
        RowViews(ImageView avatar, TextView name) {
            this.avatar = avatar;
            this.name = name;
        }
    }

    private int backgroundForRank(int rank) {
        switch (rank) {
            case 0: return R.drawable.bg_result_rank_gold;
            case 1: return R.drawable.bg_result_rank_silver;
            case 2: return R.drawable.bg_result_rank_bronze;
            default: return R.drawable.bg_result_rank_default;
        }
    }

    private String medalForRank(int rank) {
        switch (rank) {
            case 0: return "🥇";
            case 1: return "🥈";
            case 2: return "🥉";
            default: return String.valueOf(rank + 1);
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    /**
     * Choreographs the whole reveal: trophy bounces in with a soft pulsing
     * glow, title + score fade up shortly after, ranking rows cascade in one
     * by one, and a few confetti emojis float upward across the top of the
     * screen for a celebratory feel.
     */
    private void playEntranceAnimations() {
        View trophy = findViewById(R.id.tvTrophyEmoji);
        View glow   = findViewById(R.id.trophyGlow);
        View title  = findViewById(R.id.tvChallengeResult);
        View score  = findViewById(R.id.tvChallengeMyScore);
        View button = findViewById(R.id.btnBackHome);

        trophy.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_in));
        glow.setAlpha(0f);
        glow.animate().alpha(1f).setDuration(500).withEndAction(() ->
                glow.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse))
        );

        animHandler.postDelayed(() -> fadeUpIn(title), 220);
        animHandler.postDelayed(() -> fadeUpIn(score), 360);

        // staggered cascade for each ranking row
        int rowCount = scoresList.getChildCount();
        for (int i = 0; i < rowCount; i++) {
            View row = scoresList.getChildAt(i);
            long delay = 500L + (i * 130L);
            animHandler.postDelayed(() -> fadeUpIn(row), delay);
        }

        animHandler.postDelayed(() -> fadeUpIn(button), 500L + (rowCount * 130L) + 150L);

        floatConfetti(R.id.confetti1, 0);
        floatConfetti(R.id.confetti2, 180);
        floatConfetti(R.id.confetti3, 90);
        floatConfetti(R.id.confetti4, 300);
        floatConfetti(R.id.confetti5, 420);
    }

    private void fadeUpIn(View v) {
        v.setAlpha(0f);
        v.setTranslationY(dp(24));
        v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void floatConfetti(int viewId, long startDelay) {
        View v = findViewById(viewId);
        if (v == null) return;
        v.setAlpha(0f);
        v.setTranslationY(dp(30));
        v.setRotation(0f);

        Runnable cycle = new Runnable() {
            @Override
            public void run() {
                if (!confettiActive) return;
                v.animate()
                        .alpha(0.9f)
                        .translationY(-dp(40))
                        .rotation(18f)
                        .setDuration(1600)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> {
                            if (!confettiActive) return;
                            v.animate()
                                    .alpha(0f)
                                    .translationY(-dp(70))
                                    .rotation(-12f)
                                    .setDuration(1200)
                                    .withEndAction(() -> {
                                        if (!confettiActive) return;
                                        // reset to the starting position and loop again
                                        v.setTranslationY(dp(30));
                                        v.setRotation(0f);
                                        animHandler.postDelayed(this, 1400);
                                    })
                                    .start();
                        })
                        .start();
            }
        };
        animHandler.postDelayed(cycle, startDelay);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        confettiActive = false;
        animHandler.removeCallbacksAndMessages(null);
    }
}