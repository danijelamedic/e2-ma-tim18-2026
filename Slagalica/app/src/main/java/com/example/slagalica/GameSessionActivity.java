package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.games.MyNumber.MyNumberActivity;
import com.example.slagalica.games.StepByStep.StepByStepActivity;
import com.example.slagalica.games.associations.AssociationsActivity;
import com.example.slagalica.games.matching.MatchingActivity;
import com.example.slagalica.games.quiz.QuizActivity;
import com.example.slagalica.games.skocko.SkockoActivity;

public class GameSessionActivity extends AppCompatActivity {

    private int currentGameIndex = 0;
    private int totalScore = 0;

    private final Class<?>[] games = {
            QuizActivity.class,
            MatchingActivity.class,
            AssociationsActivity.class,
            SkockoActivity.class,
            StepByStepActivity.class,
            MyNumberActivity.class
    };

    private ActivityResultLauncher<Intent> gameLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null) {
                        int gameScore = result.getData().getIntExtra("score", 0);
                        totalScore += gameScore;
                    }

                    currentGameIndex++;
                    startNextGame();
                }
        );

        Toast.makeText(this, "Battle started!", Toast.LENGTH_SHORT).show();
        startNextGame();
    }

    private void startNextGame() {
        if (currentGameIndex >= games.length) {
            finishBattle();
            return;
        }

        Intent intent = new Intent(this, games[currentGameIndex]);
        intent.putExtra("isBattleMode", true);
        intent.putExtra("currentGameIndex", currentGameIndex);
        intent.putExtra("totalGames", games.length);

        gameLauncher.launch(intent);
    }

    private void finishBattle() {
        Intent intent = new Intent(this, BattleResultActivity.class);
        intent.putExtra("totalScore", totalScore);
        startActivity(intent);
        finish();
    }
}