package com.example.slagalica.games.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.slagalica.HomeActivity;
import com.example.slagalica.R;
import com.example.slagalica.models.QuizQuestion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import com.example.slagalica.data.StatisticsRepository;
import android.widget.ImageView;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;

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
    private String gameId;
    private boolean isMultiplayer;
    private ListenerRegistration quizListener;
    private String currentUid;
    private String currentTurnUid;
    private boolean isMyTurn = false;
    private boolean hasAnsweredCurrentQuestion = false;
    private long questionStartTime = 0;
    private boolean questionFinished = false;
    private boolean resultShown = false;
    private int initialScore = 0;

    private ImageView imgOpponentAvatar;
    private TextView tvOpponentName;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        db = FirebaseFirestore.getInstance();

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        gameId = getIntent().getStringExtra("gameId");
        isMultiplayer = getIntent().getBooleanExtra("isMultiplayer", false);
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
        imgOpponentAvatar = findViewById(R.id.imgOpponentAvatar);
        tvOpponentName = findViewById(R.id.tvOpponentName);

        tvQuestionProgress = findViewById(R.id.tvQuestionProgress);


        initialScore = getIntent().getIntExtra("currentTotalScore", 0);
        playerScore = initialScore;
        tvPlayerScore.setText(playerScore + " pts");

        tvBattleRound = findViewById(R.id.tvBattleRound);

        int currentGameIndex = getIntent().getIntExtra("currentGameIndex", 0);
        int totalGames = getIntent().getIntExtra("totalGames", 6);

        tvBattleRound.setText("Round " + (currentGameIndex + 1) + " / " + totalGames);

        loadCurrentUserInfo();
        loadOpponentInfo();

        answerViews = new TextView[]{
                findViewById(R.id.btnAnswer1),
                findViewById(R.id.btnAnswer2),
                findViewById(R.id.btnAnswer3),
                findViewById(R.id.btnAnswer4)
        };

        findViewById(R.id.btnQuizInfo).setOnClickListener(v -> showInfoDialog());
        findViewById(R.id.btnLeaveQuiz).setOnClickListener(v -> showLeaveDialog());
        android.view.View btnNextQuestion = findViewById(R.id.btnNextQuestion);
        btnNextQuestion.setOnClickListener(v -> goToNextQuestion());

        if (isMultiplayer) {
            btnNextQuestion.setVisibility(android.view.View.GONE);
        }


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
                    initializeQuizState();
                    listenForQuizUpdates();
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

        hasAnsweredCurrentQuestion = false;
        questionFinished = false;
        questionStartTime = System.currentTimeMillis();

        for (TextView answerView : answerViews) {
            answerView.setEnabled(true);
        }

        if (isMultiplayer) {
            tvQuestionProgress.setText("Question " + (currentQuestionIndex + 1) + "/" + questions.size() + " - both players answer");
        } else {
            tvQuestionProgress.setText("Question " + (currentQuestionIndex + 1) + "/" + questions.size());
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

                if (isMultiplayer && gameId != null) {
                    finishMultiplayerQuestionByTimeout();
                } else if (!questionAnswered) {
                    goToNextQuestionWithoutAnswer();
                }
            }
        };

        countDownTimer.start();
    }
    private void finishMultiplayerQuestionByTimeout() {
        if (questionFinished) {
            return;
        }

        checkAndScoreCurrentQuestion(true);
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

        if (isMultiplayer && hasAnsweredCurrentQuestion) {
            return;
        }

        questionAnswered = true;
        hasAnsweredCurrentQuestion = true;
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
        boolean isCorrect = selectedAnswer.equals(correctAnswer);

        if (isCorrect) {
            answerViews[selectedAnswerIndex].setBackgroundResource(R.drawable.bg_quiz_answer_correct);
        } else {
            answerViews[selectedAnswerIndex].setBackgroundResource(R.drawable.bg_quiz_answer_wrong);
            showCorrectAnswer(correctAnswer);
        }

        if (isMultiplayer && gameId != null) {
            if (isCorrect) {
                correctAnswersCount++;
            }

            saveMultiplayerAnswer(index, selectedAnswer, isCorrect);
            return;
        }

        if (isCorrect) {
            playerScore += 10;
            correctAnswersCount++;
        } else {
            playerScore -= 5;
        }

        tvPlayerScore.setText(playerScore + " pts");
    }

    private void saveMultiplayerAnswer(int index, String selectedAnswer, boolean isCorrect) {
        long answeredAt = System.currentTimeMillis();

        Map<String, Object> answerData = new HashMap<>();
        answerData.put("answerIndex", index);
        answerData.put("answerText", selectedAnswer);
        answerData.put("correct", isCorrect);
        answerData.put("answeredAt", answeredAt);

        String answerPath = "quizAnswers.question_" + currentQuestionIndex + "." + currentUid;

        db.collection("games")
                .document(gameId)
                .update(answerPath, answerData);
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

        if (isMultiplayer && gameId != null) {
            if (!questionFinished) {
                checkAndScoreCurrentQuestion(false);
                return;
            }

            advanceQuizQuestionSafely();
            return;
        }

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

    private void advanceQuizQuestionSafely() {
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference gameRef =
                    db.collection("games").document(gameId);

            com.google.firebase.firestore.DocumentSnapshot snapshot =
                    transaction.get(gameRef);

            Long firestoreQuestion = snapshot.getLong("quizQuestionIndex");

            if (firestoreQuestion == null ||
                    firestoreQuestion.intValue() != currentQuestionIndex) {
                return null;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("quizQuestionIndex", currentQuestionIndex + 1);
            updates.put("quizSelectedAnswerIndex", null);
            updates.put("quizSelectedAnswerText", null);
            updates.put("quizAnswerByUid", null);
            updates.put("quizAnsweredQuestionIndex", null);

            transaction.update(gameRef, updates);

            return null;
        });
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

        if (resultShown) {
            return;
        }

        resultShown = true;
        boolean won = correctAnswersCount >= Math.ceil(questions.size() / 2.0);

        int quizOnlyScore = playerScore - initialScore;
        StatisticsRepository.saveQuizResult(correctAnswersCount, questions.size(), quizOnlyScore, won);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (isBattleMode || isMultiplayer) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("points", playerScore);
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            imgYourAvatar.setImageResource(R.drawable.avatar_owl);
            tvPlayerName.setText("Player");
            tvPlayerInfo.setText("🪙0 ⭐0 L0");
            return;
        }

        String userId = user.getUid();

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
            case "guest":
                return R.drawable.avatar_guest;
            case "owl":
            default:
                return R.drawable.avatar_owl;
        }
    }

    private void initializeQuizState() {

        if (!isMultiplayer || gameId == null) {
            return;
        }

        db.collection("games")
                .document(gameId)
                .update(
                        "quizQuestionIndex", 0,
                        "quizPlayer1Score", 0,
                        "quizPlayer2Score", 0
                );
    }

    private void listenForQuizUpdates() {

        if (!isMultiplayer || gameId == null) {
            return;
        }

        quizListener = db.collection("games")
                .document(gameId)
                .addSnapshotListener((snapshot, e) -> {

                    if (snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    String abandonedBy = snapshot.getString("abandonedBy");
                    if (abandonedBy != null && !abandonedBy.equals(currentUid)) {
                        if (quizListener != null) { quizListener.remove(); quizListener = null; }
                        if (countDownTimer != null) { countDownTimer.cancel(); countDownTimer = null; }
                        Intent r = new Intent();
                        r.putExtra("points", 0);
                        setResult(RESULT_OK, r);
                        finish();
                        return;
                    }

                    Long questionIndex =
                            snapshot.getLong("quizQuestionIndex");
                    currentTurnUid = snapshot.getString("currentTurnUid");
                    isMyTurn = currentUid != null && currentUid.equals(currentTurnUid);

                    Long answeredQuestionIndex = snapshot.getLong("quizAnsweredQuestionIndex");
                    Long selectedAnswerIndexFromDb = snapshot.getLong("quizSelectedAnswerIndex");

                    if (!isMyTurn
                            && answeredQuestionIndex != null
                            && selectedAnswerIndexFromDb != null
                            && answeredQuestionIndex.intValue() == currentQuestionIndex) {

                        int selectedIndex = selectedAnswerIndexFromDb.intValue();

                        if (selectedIndex >= 0 && selectedIndex < answerViews.length) {
                            for (TextView answerView : answerViews) {
                                answerView.setEnabled(false);
                            }

                            answerViews[selectedIndex].setBackgroundResource(R.drawable.bg_matching_selected);
                        }
                    }

                    if (questionIndex == null) {
                        return;
                    }

                    int firestoreQuestion =
                            questionIndex.intValue();

                    if (firestoreQuestion >= questions.size()) {
                        showResultDialog();
                        return;
                    }

                    if (firestoreQuestion != currentQuestionIndex) {

                        currentQuestionIndex = firestoreQuestion;

                        if (currentQuestionIndex < questions.size()) {
                            loadQuestion();
                        }
                    }

                    if (isMultiplayer && !questionFinished) {
                        checkAndScoreCurrentQuestion(false);
                    }
                });
    }

    private void checkAndScoreCurrentQuestion(boolean allowMissingAnswers){
        if (gameId == null || currentUid == null) {
            return;
        }

        db.collection("games").document(gameId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        return;
                    }

                    Long currentGameQuestion = snapshot.getLong("quizQuestionIndex");
                    if (currentGameQuestion == null ||
                            currentGameQuestion.intValue() != currentQuestionIndex) {
                        return;
                    }

                    Boolean alreadyScored = snapshot.getBoolean(
                            "quizScored.question_" + currentQuestionIndex
                    );

                    if (Boolean.TRUE.equals(alreadyScored)) {
                        return;
                    }

                    String player1 = snapshot.getString("player1");
                    String player2 = snapshot.getString("player2");

                    if (player1 == null || player2 == null) {
                        return;
                    }

                    Object answer1Obj = snapshot.get(
                            "quizAnswers.question_" + currentQuestionIndex + "." + player1
                    );

                    Object answer2Obj = snapshot.get(
                            "quizAnswers.question_" + currentQuestionIndex + "." + player2
                    );

                    if (!allowMissingAnswers && (answer1Obj == null || answer2Obj == null)) {
                        return;
                    }

                    scoreQuestionTransaction(player1, player2, allowMissingAnswers);
                });
    }

    private void scoreQuestionTransaction(String player1, String player2, boolean allowMissingAnswers) {
        final boolean[] didScore = {false};

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference gameRef =
                    db.collection("games").document(gameId);

            com.google.firebase.firestore.DocumentSnapshot snapshot =
                    transaction.get(gameRef);

            Boolean alreadyScored = snapshot.getBoolean(
                    "quizScored.question_" + currentQuestionIndex
            );

            if (Boolean.TRUE.equals(alreadyScored)) {
                didScore[0] = false;
                return null;
            }

            Map<String, Object> answer1 =
                    (Map<String, Object>) snapshot.get(
                            "quizAnswers.question_" + currentQuestionIndex + "." + player1
                    );

            Map<String, Object> answer2 =
                    (Map<String, Object>) snapshot.get(
                            "quizAnswers.question_" + currentQuestionIndex + "." + player2
                    );

            if (!allowMissingAnswers && (answer1 == null || answer2 == null)) {
                didScore[0] = false;
                return null;
            }

            boolean p1Answered = answer1 != null;
            boolean p2Answered = answer2 != null;

            boolean p1Correct = p1Answered && Boolean.TRUE.equals(answer1.get("correct"));
            boolean p2Correct = p2Answered && Boolean.TRUE.equals(answer2.get("correct"));

            long p1Time = p1Answered ? ((Number) answer1.get("answeredAt")).longValue() : Long.MAX_VALUE;
            long p2Time = p2Answered ? ((Number) answer2.get("answeredAt")).longValue() : Long.MAX_VALUE;

            long p1Delta = 0;
            long p2Delta = 0;

            if (p1Answered && !p1Correct) {
                p1Delta -= 5;
            }

            if (p2Answered && !p2Correct) {
                p2Delta -= 5;
            }

            if (p1Correct && p2Correct) {
                if (p1Time <= p2Time) {
                    p1Delta += 10;
                } else {
                    p2Delta += 10;
                }
            } else if (p1Correct) {
                p1Delta += 10;
            } else if (p2Correct) {
                p2Delta += 10;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("score1", FieldValue.increment(p1Delta));
            updates.put("score2", FieldValue.increment(p2Delta));
            updates.put("quizScored.question_" + currentQuestionIndex, true);
            updates.put("quizScoredBy.question_" + currentQuestionIndex, currentUid);

            transaction.update(gameRef, updates);

            didScore[0] = true;
            return null;
        }).addOnSuccessListener(unused -> {
            if (!didScore[0]) {
                return;
            }

            questionFinished = true;

            db.collection("games").document(gameId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String player1Id = snapshot.getString("player1");
                        Long score1 = snapshot.getLong("score1");
                        Long score2 = snapshot.getLong("score2");

                        if (currentUid.equals(player1Id)) {
                            playerScore = score1 != null ? score1.intValue() : 0;
                        } else {
                            playerScore = score2 != null ? score2.intValue() : 0;
                        }

                        tvPlayerScore.setText(playerScore + " pts");

                        if (currentQuestionIndex < questions.size() - 1) {
                            advanceQuizQuestionSafely();
                        } else {
                            db.collection("games")
                                    .document(gameId)
                                    .update("quizQuestionIndex", questions.size())
                                    .addOnSuccessListener(done -> showResultDialog());
                        }
                    });
        });
    }

    private void loadOpponentInfo() {
        if (!isMultiplayer || gameId == null || currentUid == null) {
            return;
        }

        db.collection("games")
                .document(gameId)
                .get()
                .addOnSuccessListener(gameSnapshot -> {

                    String player1 = gameSnapshot.getString("player1");
                    String player2 = gameSnapshot.getString("player2");

                    if (player1 == null || player2 == null) {
                        return;
                    }

                    String opponentUid = currentUid.equals(player1) ? player2 : player1;

                    db.collection("users")
                            .document(opponentUid)
                            .get()
                            .addOnSuccessListener(userSnapshot -> {
                                if (!userSnapshot.exists()) {
                                    Toast.makeText(this, "Opponent user document not found", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                String username = userSnapshot.getString("username");
                                String avatar = userSnapshot.getString("avatar");

                                tvOpponentName.setText(username != null ? username : "Opponent");
                                imgOpponentAvatar.setImageResource(getAvatarResource(avatar));

                                Long tokens = userSnapshot.getLong("tokens");
                                Long stars = userSnapshot.getLong("stars");
                                Long league = userSnapshot.getLong("league");

                                long tokensValue = tokens != null ? tokens : 0;
                                long starsValue = stars != null ? stars : 0;
                                long leagueValue = league != null ? league : 0;

                                tvOpponentInfo.setText(
                                        "🪙" + tokensValue +
                                                " ⭐" + starsValue +
                                                " L" + leagueValue
                                );
                            });
                });
    }

}