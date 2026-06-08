package com.example.slagalica.data;

import com.example.slagalica.models.QuizQuestion;
import com.example.slagalica.models.MatchingGame;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseSeeder {

    public static void seedQuizQuestions() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        QuizQuestion q1 = new QuizQuestion(
                "Which planet is known as the Red Planet?",
                "Earth",
                "Mars",
                "Jupiter",
                "Venus",
                "Mars"
        );

        QuizQuestion q2 = new QuizQuestion(
                "What is the capital of Italy?",
                "Rome",
                "Paris",
                "Berlin",
                "Madrid",
                "Rome"
        );

        QuizQuestion q3 = new QuizQuestion(
                "How many days are there in a leap year?",
                "365",
                "366",
                "364",
                "360",
                "366"
        );

        QuizQuestion q4 = new QuizQuestion(
                "Which ocean is the largest?",
                "Atlantic Ocean",
                "Indian Ocean",
                "Pacific Ocean",
                "Arctic Ocean",
                "Pacific Ocean"
        );

        QuizQuestion q5 = new QuizQuestion(
                "Who wrote Romeo and Juliet?",
                "William Shakespeare",
                "Charles Dickens",
                "Mark Twain",
                "Dante Alighieri",
                "William Shakespeare"
        );

        db.collection("quizQuestions").document("q1").set(q1);
        db.collection("quizQuestions").document("q2").set(q2);
        db.collection("quizQuestions").document("q3").set(q3);
        db.collection("quizQuestions").document("q4").set(q4);
        db.collection("quizQuestions").document("q5").set(q5);
    }

    public static void seedMatchingGames() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        MatchingGame matchingGame = new MatchingGame();

        matchingGame.setLeft1("Serbia");
        matchingGame.setLeft2("Italy");
        matchingGame.setLeft3("France");
        matchingGame.setLeft4("Germany");
        matchingGame.setLeft5("Spain");

        matchingGame.setRight1("Berlin");
        matchingGame.setRight2("Madrid");
        matchingGame.setRight3("Belgrade");
        matchingGame.setRight4("Rome");
        matchingGame.setRight5("Paris");

        matchingGame.setMatch1("Belgrade");
        matchingGame.setMatch2("Rome");
        matchingGame.setMatch3("Paris");
        matchingGame.setMatch4("Berlin");
        matchingGame.setMatch5("Madrid");

        db.collection("matchingGames").document("game1").set(matchingGame);
    }
}