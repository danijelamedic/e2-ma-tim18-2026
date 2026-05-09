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


public class QuizActivity extends AppCompatActivity {

    private TextView tvQuestionCounter;
    private TextView tvQuestion;
    private TextView tvPlayerScore;
    private TextView[] answerViews;
    private int currentQuestionIndex = 0;
    private int selectedAnswerIndex = -1;
    private int playerScore = 0;
    private final String[] questions = {
            "Which planet is known as the Red Planet?",
            "What is the capital of Italy?",
            "How many days are there in a leap year?",
            "Which ocean is the largest?",
            "Who wrote Romeo and Juliet?"
    };
    private final String[][] answers = {
            {"Earth", "Mars", "Jupiter", "Venus"},
            {"Rome", "Paris", "Berlin", "Madrid"},
            {"365", "366", "364", "360"},
            {"Atlantic Ocean", "Indian Ocean", "Pacific Ocean", "Arctic Ocean"},
            {"William Shakespeare", "Charles Dickens", "Mark Twain", "Dante Alighieri"}
    };
    private final int[] correctAnswers = {1, 0, 1, 2, 0};
    private TextView tvQuizTimer;
    private CountDownTimer countDownTimer;
    private boolean questionAnswered = false;
    private long timeLeftMillis = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvQuestionCounter = findViewById(R.id.tvQuestionCounter);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvPlayerScore = findViewById(R.id.tvPlayerScore);
        tvQuizTimer = findViewById(R.id.tvQuizTimer);

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

        loadQuestion();
    }

    private void loadQuestion() {
        selectedAnswerIndex = -1;
        questionAnswered = false;

        tvQuestionCounter.setText("Question " + (currentQuestionIndex + 1) + "/5");
        tvQuestion.setText(questions[currentQuestionIndex]);

        for (int i = 0; i < answerViews.length; i++) {
            answerViews[i].setText(answers[currentQuestionIndex][i]);
            answerViews[i].setSelected(false);
            answerViews[i].setEnabled(true);
            answerViews[i].setBackgroundResource(R.drawable.bg_quiz_answer);
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

        int correctIndex = correctAnswers[currentQuestionIndex];

        if (selectedAnswerIndex == correctIndex) {
            playerScore += 10;
            answerViews[selectedAnswerIndex].setBackgroundResource(R.drawable.bg_quiz_answer_correct);
        } else {
            playerScore -= 5;
            answerViews[selectedAnswerIndex].setBackgroundResource(R.drawable.bg_quiz_answer_wrong);
            answerViews[correctIndex].setBackgroundResource(R.drawable.bg_quiz_answer_correct);
        }

        tvPlayerScore.setText(playerScore + " pts");
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

        if (currentQuestionIndex < questions.length - 1) {
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
                .setTitle("Leave game")
                .setMessage("Are you sure you want to leave the game?")
                .setPositiveButton("YES", (dialog, which) -> {
                    Intent intent = new Intent(QuizActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("NO", null)
                .show();
    }

    private void showResultDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Quiz finished")
                .setMessage("Your score: " + playerScore + " pts")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(QuizActivity.this, HomeActivity.class);
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}