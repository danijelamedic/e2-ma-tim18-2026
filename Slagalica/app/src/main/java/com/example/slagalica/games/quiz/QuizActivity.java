package com.example.slagalica.games.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.slagalica.HomeActivity;
import com.example.slagalica.R;
import com.example.slagalica.models.QuizQuestion;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import com.example.slagalica.data.StatisticsRepository;
import android.widget.ImageView;

public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestion;
    private TextView tvPlayerScore;
    private TextView[] answerViews;

    private final List<QuizQuestion> questions = new ArrayList<>();

    private int currentQuestionIndex = 0;
    private int selectedAnswerIndex = -1;
    private int playerScore = 0;
    private int correctAnswersCount = 0;

    private TextView tvQuizTimer;
    private CountDownTimer countDownTimer;
    private boolean questionAnswered = false;
    private long timeLeftMillis = 5000;
    private boolean isBattleMode;
    private ImageView imgYourAvatar;
    private TextView tvPlayerName;
    private TextView tvOpponentInfo;
    private TextView tvPlayerInfo;
    private TextView tvQuestionProgress;
    private TextView tvBattleRound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        isBattleMode = getIntent().getBooleanExtra("isBattleMode", false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvQuestion = findViewById(R.id.tvQuestion);
        tvPlayerScore = findViewById(R.id.tvPlayerScore);
        tvQuizTimer = findViewById(R.id.tvQuizTimer);
        imgYourAvatar = findViewById(R.id.imgYourAvatar);
        tvPlayerInfo = findViewById(R.id.tvPlayerInfo);
        tvPlayerName = findViewById(R.id.tvPlayerName);

        tvOpponentInfo = findViewById(R.id.tvOpponentInfo);
        tvOpponentInfo.setText("🪙0 ⭐0 L0");

        tvQuestionProgress = findViewById(R.id.tvQuestionProgress);


        playerScore = getIntent().getIntExtra("currentTotalScore", 0);
        tvPlayerScore.setText(playerScore + " pts");

        tvBattleRound = findViewById(R.id.tvBattleRound);

        int currentGameIndex = getIntent().getIntExtra("currentGameIndex", 0);
        int totalGames = getIntent().getIntExtra("totalGames", 6);

        tvBattleRound.setText("Round " + (currentGameIndex + 1) + " / " + totalGames);

        loadCurrentUserInfo();

        answerViews = new TextView[]{
                findViewById(R.id.btnAnswer1),
                findViewById(R.id.btnAnswer2),
                findViewById(R.id.btnAnswer3),
                findViewById(R.id.btnAnswer4)
        };

        findViewById(R.id.btnQuizInfo).setOnClickListener(v -> showInfoDialog());
        findViewById(R.id.btnLeaveQuiz).setOnClickListener(v -> showLeaveDialog());
        findViewById(R.id.btnNextQuestion).setOnClickListener(v -> goToNextQuestion());

        for (int i = 0; i < answerViews.length; i++) {
            final int index = i;
            answerViews[i].setOnClickListener(v -> selectAnswer(index));
        }

        loadQuestionsFromFirebase();
    }

    private void loadQuestionsFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("quizQuestions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    questions.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        QuizQuestion question = document.toObject(QuizQuestion.class);
                        questions.add(question);
                    }

                    if (questions.isEmpty()) {
                        Toast.makeText(this, "No quiz questions found.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentQuestionIndex = 0;
                    loadQuestion();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load questions.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadQuestion() {
        selectedAnswerIndex = -1;
        questionAnswered = false;

        QuizQuestion currentQuestion = questions.get(currentQuestionIndex);

        String questionProgress =
                "Question " + (currentQuestionIndex + 1) + "/" + questions.size();

        tvQuestionProgress.setText(questionProgress);

        tvQuestion.setText(currentQuestion.getQuestion());

        answerViews[0].setText(currentQuestion.getOptionA());
        answerViews[1].setText(currentQuestion.getOptionB());
        answerViews[2].setText(currentQuestion.getOptionC());
        answerViews[3].setText(currentQuestion.getOptionD());

        for (TextView answerView : answerViews) {
            answerView.setSelected(false);
            answerView.setEnabled(true);
            answerView.setBackgroundResource(R.drawable.bg_quiz_answer);
        }

        timeLeftMillis = 5000;
        startTimer();
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(timeLeftMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
                int secondsLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                tvQuizTimer.setText("⏱ " + secondsLeft + "s");
            }

            @Override
            public void onFinish() {
                timeLeftMillis = 0;
                tvQuizTimer.setText("⏱ 0s");

                if (!questionAnswered) {
                    goToNextQuestionWithoutAnswer();
                }
            }
        };

        countDownTimer.start();
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void selectAnswer(int index) {
        if (questionAnswered) {
            return;
        }

        questionAnswered = true;
        selectedAnswerIndex = index;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        for (TextView answerView : answerViews) {
            answerView.setEnabled(false);
            answerView.setSelected(false);
        }

        QuizQuestion currentQuestion = questions.get(currentQuestionIndex);
        String selectedAnswer = answerViews[selectedAnswerIndex].getText().toString();
        String correctAnswer = currentQuestion.getCorrectAnswer();

        if (selectedAnswer.equals(correctAnswer)) {
            playerScore += 10;
            correctAnswersCount++;
            answerViews[selectedAnswerIndex].setBackgroundResource(R.drawable.bg_quiz_answer_correct);
        } else {
            playerScore -= 5;
            answerViews[selectedAnswerIndex].setBackgroundResource(R.drawable.bg_quiz_answer_wrong);
            showCorrectAnswer(correctAnswer);
        }

        tvPlayerScore.setText(playerScore + " pts");
    }

    private void showCorrectAnswer(String correctAnswer) {
        for (TextView answerView : answerViews) {
            if (answerView.getText().toString().equals(correctAnswer)) {
                answerView.setBackgroundResource(R.drawable.bg_quiz_answer_correct);
                break;
            }
        }
    }

    private void goToNextQuestion() {
        if (!questionAnswered) {
            Toast.makeText(this, "Please select an answer first.", Toast.LENGTH_SHORT).show();
            return;
        }

        moveToNextQuestion();
    }

    private void goToNextQuestionWithoutAnswer() {
        Toast.makeText(this, "Time is up! 0 pts", Toast.LENGTH_SHORT).show();
        moveToNextQuestion();
    }

    private void moveToNextQuestion() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
            loadQuestion();
        } else {
            showResultDialog();
        }
    }

    private void showInfoDialog() {
        pauseTimer();

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.quiz_rules_title))
                .setMessage(getString(R.string.quiz_rules_message))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    dialog.dismiss();

                    if (timeLeftMillis > 0 && !questionAnswered) {
                        startTimer();
                    }
                })
                .show();
    }

    private void showLeaveDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave battle")
                .setMessage("If you leave now, you will lose the entire battle. Are you sure?")
                .setPositiveButton("YES", (dialog, which) -> {
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }

                    if (isBattleMode) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("battleLost", true);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Intent intent = new Intent(QuizActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("NO", null)
                .show();
    }

    private void showResultDialog() {
        boolean won = correctAnswersCount >= Math.ceil(questions.size() / 2.0);

        StatisticsRepository.saveQuizResult(correctAnswersCount, questions.size(), won);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (isBattleMode) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("score", playerScore);
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Quiz finished")
                .setMessage("Your score: " + playerScore + " pts")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(QuizActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void loadCurrentUserInfo() {
        String userId = "jMwwl0MoswM7u5nifYChTng97jj1";

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        imgYourAvatar.setImageResource(R.drawable.avatar_owl);
                        tvPlayerName.setText("Player");
                        tvPlayerInfo.setText("🪙0 ⭐0 L0");
                        return;
                    }

                    String username = document.getString("username");
                    String avatar = document.getString("avatar");

                    Long tokens = document.getLong("tokens");
                    Long stars = document.getLong("stars");
                    Long league = document.getLong("league");

                    long tokensValue = tokens != null ? tokens : 0;
                    long starsValue = stars != null ? stars : 0;
                    long leagueValue = league != null ? league : 0;

                    tvPlayerName.setText(username != null ? username : "Player");
                    imgYourAvatar.setImageResource(getAvatarResource(avatar));

                    tvPlayerInfo.setText(
                            "🪙" + tokensValue +
                                    " ⭐" + starsValue +
                                    " L" + leagueValue
                    );
                })
                .addOnFailureListener(e -> {
                    imgYourAvatar.setImageResource(R.drawable.avatar_owl);
                    tvPlayerName.setText("Player");
                    tvPlayerInfo.setText("🪙0 ⭐0 L0");
                });
    }

    private int getAvatarResource(String avatar) {
        if (avatar == null) {
            return R.drawable.avatar_owl;
        }

        switch (avatar) {
            case "fox":
                return R.drawable.avatar_fox;
            case "penguin":
                return R.drawable.avatar_penguin;
            case "wolf":
                return R.drawable.avatar_wolf;
            case "cat":
                return R.drawable.avatar_cat;
            case "dog":
                return R.drawable.avatar_dog;
            case "owl":
            default:
                return R.drawable.avatar_owl;
        }
    }

}