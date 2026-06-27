package com.example.slagalica.tournament;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.GameActivity;
import com.example.slagalica.HomeActivity;
import com.example.slagalica.R;
import com.example.slagalica.data.PlayerProfileLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class TournamentActivity extends AppCompatActivity {

    private final TournamentRepository repository = new TournamentRepository();
    private String currentUid;
    private String tournamentId;
    private ListenerRegistration queueListener;
    private ListenerRegistration tournamentListener;
    private LinearLayout content;
    private TextView title;
    private TextView status;
    private TextView helperText;
    private Button primaryButton;
    private Button secondaryButton;
    private boolean gameStarted = false;
    private boolean canUseBackToHome = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        tournamentId = getIntent().getStringExtra("tournamentId");

        buildLayout();
        if (tournamentId != null) {
            listenTournament(tournamentId);
        } else {
            showJoinState();
        }
    }

    private void buildLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFFF6F3FF);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(16));
        scroll.addView(content);

        title = new TextView(this);
        title.setText("🏆 TOURNAMENT");
        title.setTextColor(0xFF2D1B5E);
        title.setTextSize(28);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD_ITALIC));
        title.setGravity(Gravity.START);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.setMargins(0, 0, 0, dp(10));
        content.addView(title, titleParams);

        status = new TextView(this);
        status.setTextColor(0xFF4C2A91);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(4), 0, dp(6));
        content.addView(status, matchWrap());

        helperText = new TextView(this);
        helperText.setTextColor(0xFF6E5A9E);
        helperText.setTextSize(14);
        helperText.setGravity(Gravity.CENTER);
        helperText.setPadding(dp(8), 0, dp(8), dp(6));
        content.addView(helperText, matchWrap());

        primaryButton = makeButton("Join tournament");
        secondaryButton = makeButton("Cancel");
        content.addView(primaryButton);
        content.addView(secondaryButton);

        setContentView(scroll);
    }

    private void showJoinState() {
        canUseBackToHome = true;
        clearBracketViews();
        status.setText("Entry costs 3 tokens. Four players are matched into two semifinals, and the winners meet in the final.");
        helperText.setText("");
        primaryButton.setText("Join tournament");
        primaryButton.setEnabled(true);
        primaryButton.setVisibility(View.VISIBLE);
        primaryButton.setOnClickListener(v -> joinTournament());
        secondaryButton.setVisibility(View.GONE);
    }

    private void joinTournament() {
        primaryButton.setEnabled(false);
        status.setText("Joining tournament queue...");
        repository.joinTournament(currentUid, new TournamentRepository.SimpleCallback() {
            @Override
            public void onSuccess(String id) {
                listenQueue();
            }

            @Override
            public void onError(String message) {
                primaryButton.setEnabled(true);
                status.setText(message);
                Toast.makeText(TournamentActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void listenQueue() {
        if (queueListener != null) queueListener.remove();
        canUseBackToHome = true;
        status.setText("Waiting for 4 active players...");
        helperText.setText("If you cancel before the bracket is formed, your 3 tournament tokens will be refunded.");
        repository.checkForReadyTournament();
        primaryButton.setVisibility(View.GONE);
        secondaryButton.setVisibility(View.VISIBLE);
        secondaryButton.setText("Cancel");
        secondaryButton.setOnClickListener(v -> {
            repository.cancelWaiting(currentUid);
            goHome();
        });

        queueListener = repository.listenQueue(currentUid, new TournamentRepository.TournamentSnapshotCallback() {
            @Override
            public void onSnapshot(DocumentSnapshot snapshot) {
                if (snapshot == null || !snapshot.exists()) return;
                String foundTournamentId = snapshot.getString("tournamentId");
                if (foundTournamentId != null) {
                    tournamentId = foundTournamentId;
                    listenTournament(foundTournamentId);
                } else {
                    repository.checkForReadyTournament();
                }
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(TournamentActivity.this, "Tournament queue error.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenTournament(String id) {
        if (tournamentListener != null) tournamentListener.remove();
        tournamentListener = repository.listenTournament(id, new TournamentRepository.TournamentSnapshotCallback() {
            @Override
            public void onSnapshot(DocumentSnapshot snapshot) {
                if (snapshot == null || !snapshot.exists()) return;
                renderTournament(snapshot);
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(TournamentActivity.this, "Tournament loading error.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void renderTournament(DocumentSnapshot tournament) {
        clearBracketViews();
        String tournamentStatus = tournament.getString("status");
        status.setText("Bracket is ready. Start your available match when it appears.");
        helperText.setText("");

        addSectionTitle("Semifinals");
        addMatchRow("Semifinal 1", (List<String>) tournament.get("semifinal1Players"),
                tournament.getString("semifinal1Winner"));
        addMatchRow("Semifinal 2", (List<String>) tournament.get("semifinal2Players"),
                tournament.getString("semifinal2Winner"));

        addSectionTitle("Final");
        addMatchRow("Final", (List<String>) tournament.get("finalPlayers"),
                tournament.getString("winnerUid"));

        String gameId = getPlayableGameId(tournament);
        if (gameId != null) {
            primaryButton.setText("Start tournament match");
            primaryButton.setEnabled(true);
            primaryButton.setVisibility(View.VISIBLE);
            primaryButton.setOnClickListener(v -> openGame(gameId));
        } else {
            primaryButton.setVisibility(View.GONE);
        }

        if ("finished".equals(tournamentStatus)) {
            canUseBackToHome = true;
            String winnerUid = tournament.getString("winnerUid");
            status.setText(currentUid.equals(winnerUid)
                    ? "Tournament complete. You are the champion."
                    : "Tournament complete.");
            secondaryButton.setVisibility(View.VISIBLE);
            secondaryButton.setText("Home");
            secondaryButton.setOnClickListener(v -> goHome());
        } else if (isEliminated(tournament)) {
            canUseBackToHome = true;
            status.setText("You are eliminated. You can return home.");
            secondaryButton.setVisibility(View.VISIBLE);
            secondaryButton.setText("Home");
            secondaryButton.setOnClickListener(v -> goHome());
        } else if (gameId == null) {
            canUseBackToHome = false;
            status.setText("Waiting for the next tournament match.");
            secondaryButton.setVisibility(View.GONE);
        } else {
            canUseBackToHome = false;
            status.setText("Your tournament match is ready.");
            secondaryButton.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("unchecked")
    private String getPlayableGameId(DocumentSnapshot tournament) {
        List<String> semi1 = (List<String>) tournament.get("semifinal1Players");
        List<String> semi2 = (List<String>) tournament.get("semifinal2Players");
        List<String> finalPlayers = (List<String>) tournament.get("finalPlayers");

        if (contains(semi1, currentUid) && tournament.getString("semifinal1Winner") == null) {
            return tournament.getString("semifinal1GameId");
        }
        if (contains(semi2, currentUid) && tournament.getString("semifinal2Winner") == null) {
            return tournament.getString("semifinal2GameId");
        }
        if (contains(finalPlayers, currentUid) && tournament.getString("winnerUid") == null) {
            return tournament.getString("finalGameId");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private boolean isEliminated(DocumentSnapshot tournament) {
        List<String> semi1 = (List<String>) tournament.get("semifinal1Players");
        List<String> semi2 = (List<String>) tournament.get("semifinal2Players");
        String semi1Winner = tournament.getString("semifinal1Winner");
        String semi2Winner = tournament.getString("semifinal2Winner");
        String finalWinner = tournament.getString("winnerUid");

        if (contains(semi1, currentUid) && semi1Winner != null && !currentUid.equals(semi1Winner)) {
            return true;
        }
        if (contains(semi2, currentUid) && semi2Winner != null && !currentUid.equals(semi2Winner)) {
            return true;
        }
        return finalWinner != null && !currentUid.equals(finalWinner);
    }

    private void openGame(String gameId) {
        if (gameStarted) return;
        gameStarted = true;
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("gameId", gameId);
        startActivity(intent);
        finish();
    }

    private void addSectionTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xFF2D1B5E);
        view.setTextSize(20);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, dp(8), 0, dp(4));
        content.addView(view, content.getChildCount() - 2, matchWrap());
    }

    private void addMatchRow(String label, List<String> players, String winnerUid) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        card.setBackgroundResource(R.drawable.bg_profile_card);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(0xFF4C2A91);
        labelView.setTextSize(15);
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(labelView, matchWrap());

        if (players == null || players.isEmpty()) {
            TextView pending = new TextView(this);
            pending.setText("Waiting for finalists...");
            pending.setTextColor(0xFF6E5A9E);
            pending.setPadding(0, dp(4), 0, 0);
            card.addView(pending, matchWrap());
        } else {
            for (String uid : players) {
                addPlayerRow(card, uid, uid.equals(winnerUid));
            }
        }

        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, 0, 0, dp(6));
        content.addView(card, content.getChildCount() - 2, lp);
    }

    private void addPlayerRow(LinearLayout parent, String uid, boolean winner) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, 0);

        ImageView avatar = new ImageView(this);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        row.addView(avatar, avatarLp);

        TextView text = new TextView(this);
        text.setText(uid.equals(currentUid) ? "You" : "Loading...");
        text.setTextColor(winner ? 0xFF126A32 : 0xFF2D1B5E);
        text.setTextSize(14);
        text.setTypeface(Typeface.DEFAULT, winner ? Typeface.BOLD : Typeface.NORMAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMargins(dp(10), 0, 0, 0);
        row.addView(text, textLp);

        parent.addView(row, matchWrap());

        PlayerProfileLoader.load(uid, summary -> {
            avatar.setImageResource(summary.avatarResId);
            String prefix = uid.equals(currentUid) ? "You - " : "";
            String suffix = winner ? "  WIN" : "";
            text.setText(prefix + summary.username + "  " + summary.info.replace("\n", "  ") + suffix);
        });
    }

    private void clearBracketViews() {
        while (content.getChildCount() > 5) {
            content.removeViewAt(3);
        }
    }

    private boolean contains(List<String> list, String uid) {
        return list != null && uid != null && list.contains(uid);
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackgroundResource(R.drawable.bg_purple_button);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, dp(10), 0, 0);
        button.setLayoutParams(lp);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (canUseBackToHome) {
            goHome();
            return;
        }

        Toast.makeText(this, "Tournament is active.", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (queueListener != null) queueListener.remove();
        if (tournamentListener != null) tournamentListener.remove();
        super.onDestroy();
    }
}
