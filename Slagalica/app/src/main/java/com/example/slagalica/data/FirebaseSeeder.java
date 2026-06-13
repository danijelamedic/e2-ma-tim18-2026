package com.example.slagalica.data;

import com.example.slagalica.models.AssociationGame;
import com.example.slagalica.models.MatchingGame;
import com.example.slagalica.models.QuizQuestion;
import com.example.slagalica.models.SkockoGame;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

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
        QuizQuestion q6 = new QuizQuestion(
                "What is the largest mammal in the world?",
                "Elephant",
                "Blue Whale",
                "Giraffe",
                "Hippopotamus",
                "Blue Whale"
        );

        QuizQuestion q7 = new QuizQuestion(
                "Which country is famous for the Eiffel Tower?",
                "Italy",
                "France",
                "Spain",
                "Germany",
                "France"
        );

        QuizQuestion q8 = new QuizQuestion(
                "How many continents are there?",
                "5",
                "6",
                "7",
                "8",
                "7"
        );

        QuizQuestion q9 = new QuizQuestion(
                "Which gas do plants absorb from the atmosphere?",
                "Oxygen",
                "Carbon Dioxide",
                "Nitrogen",
                "Hydrogen",
                "Carbon Dioxide"
        );

        QuizQuestion q10 = new QuizQuestion(
                "Who painted the Mona Lisa?",
                "Pablo Picasso",
                "Vincent van Gogh",
                "Leonardo da Vinci",
                "Michelangelo",
                "Leonardo da Vinci"
        );

        db.collection("quizQuestions").document("q1").set(q1);
        db.collection("quizQuestions").document("q2").set(q2);
        db.collection("quizQuestions").document("q3").set(q3);
        db.collection("quizQuestions").document("q4").set(q4);
        db.collection("quizQuestions").document("q5").set(q5);
        db.collection("quizQuestions").document("q6").set(q6);
        db.collection("quizQuestions").document("q7").set(q7);
        db.collection("quizQuestions").document("q8").set(q8);
        db.collection("quizQuestions").document("q9").set(q9);
        db.collection("quizQuestions").document("q10").set(q10);
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

        MatchingGame matchingGame2 = new MatchingGame();

        matchingGame2.setLeft1("Dog");
        matchingGame2.setLeft2("Cat");
        matchingGame2.setLeft3("Cow");
        matchingGame2.setLeft4("Sheep");
        matchingGame2.setLeft5("Horse");

        matchingGame2.setRight1("Meow");
        matchingGame2.setRight2("Neigh");
        matchingGame2.setRight3("Bark");
        matchingGame2.setRight4("Moo");
        matchingGame2.setRight5("Baa");

        matchingGame2.setMatch1("Bark");
        matchingGame2.setMatch2("Meow");
        matchingGame2.setMatch3("Moo");
        matchingGame2.setMatch4("Baa");
        matchingGame2.setMatch5("Neigh");

        db.collection("matchingGames").document("game2").set(matchingGame2);


        MatchingGame matchingGame3 = new MatchingGame();

        matchingGame3.setLeft1("Java");
        matchingGame3.setLeft2("Android");
        matchingGame3.setLeft3("Firebase");
        matchingGame3.setLeft4("HTML");
        matchingGame3.setLeft5("Git");

        matchingGame3.setRight1("Version control");
        matchingGame3.setRight2("Web page structure");
        matchingGame3.setRight3("Programming language");
        matchingGame3.setRight4("Mobile platform");
        matchingGame3.setRight5("Cloud database");

        matchingGame3.setMatch1("Programming language");
        matchingGame3.setMatch2("Mobile platform");
        matchingGame3.setMatch3("Cloud database");
        matchingGame3.setMatch4("Web page structure");
        matchingGame3.setMatch5("Version control");

        db.collection("matchingGames").document("game3").set(matchingGame3);
    }

    public static void seedAssociationGames() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<AssociationGame> games = Arrays.asList(
                association(
                        columns(
                                clues("NILE", "DANUBE", "THAMES", "AMAZON"),
                                clues("SEA", "LAKE", "RIVER", "STREAM"),
                                clues("SOURCE", "MOUTH", "BANK", "CURRENT"),
                                clues("RAIN", "SNOW", "ICE", "STEAM")
                        ),
                        clues("RIVERS", "WATER", "FLOW", "STATES OF MATTER"),
                        "HYDROLOGY"
                ),
                association(
                        columns(
                                clues("APPLE", "PEAR", "PLUM", "PEACH"),
                                clues("CARROT", "POTATO", "TOMATO", "PEPPER"),
                                clues("RICE", "WHEAT", "CORN", "OATS"),
                                clues("SALT", "PEPPER", "OREGANO", "BASIL")
                        ),
                        clues("FRUIT", "VEGETABLES", "GRAINS", "SPICES"),
                        "FOOD"
                ),
                association(
                        columns(
                                clues("JAVA", "KOTLIN", "PYTHON", "C++"),
                                clues("ANDROID", "IOS", "WINDOWS", "LINUX"),
                                clues("GIT", "COMMIT", "BRANCH", "MERGE"),
                                clues("HTML", "CSS", "JAVASCRIPT", "REACT")
                        ),
                        clues("PROGRAMMING LANGUAGES", "OPERATING SYSTEMS", "VERSION CONTROL", "WEB"),
                        "PROGRAMMING"
                ),
                association(
                        columns(
                                clues("NOVAK", "RAFAEL", "ROGER", "CARLOS"),
                                clues("RACKET", "BALL", "NET", "COURT"),
                                clues("WIMBLEDON", "ROLAND GARROS", "US OPEN", "AUSTRALIAN OPEN"),
                                clues("SET", "GAME", "SERVE", "BREAK")
                        ),
                        clues("TENNIS PLAYERS", "EQUIPMENT", "GRAND SLAM", "RULES"),
                        "TENNIS"
                ),
                association(
                        columns(
                                clues("BELGRADE", "NOVI SAD", "NIS", "KRAGUJEVAC"),
                                clues("KALEMEGDAN", "PETROVARADIN", "SKULL TOWER", "SUMARICE"),
                                clues("DANUBE", "SAVA", "MORAVA", "TISA"),
                                clues("KOPAONIK", "ZLATIBOR", "TARA", "FRUSKA GORA")
                        ),
                        clues("CITIES", "LANDMARKS", "RIVERS", "MOUNTAINS"),
                        "SERBIA"
                ),
                association(
                        columns(
                                clues("MERCURY", "VENUS", "EARTH", "MARS"),
                                clues("SUN", "MOON", "STAR", "COMET"),
                                clues("GALILEO", "NEWTON", "EINSTEIN", "TESLA"),
                                clues("TELESCOPE", "ROCKET", "SATELLITE", "PROBE")
                        ),
                        clues("PLANETS", "CELESTIAL BODIES", "SCIENTISTS", "EQUIPMENT"),
                        "SPACE"
                ),
                association(
                        columns(
                                clues("VIOLIN", "PIANO", "GUITAR", "DRUMS"),
                                clues("NOTE", "RHYTHM", "MELODY", "CHORD"),
                                clues("ROCK", "POP", "JAZZ", "CLASSICAL"),
                                clues("CONCERT", "ALBUM", "SINGLE", "TOUR")
                        ),
                        clues("INSTRUMENTS", "MUSIC TERMS", "GENRES", "RELEASES"),
                        "MUSIC"
                ),
                association(
                        columns(
                                clues("RED", "BLUE", "GREEN", "YELLOW"),
                                clues("CIRCLE", "SQUARE", "TRIANGLE", "RECTANGLE"),
                                clues("BRUSH", "CANVAS", "PALETTE", "PAINT"),
                                clues("MONA LISA", "GUERNICA", "STARRY NIGHT", "THE LAST SUPPER")
                        ),
                        clues("COLORS", "SHAPES", "PAINTING", "ARTWORKS"),
                        "ART"
                ),
                association(
                        columns(
                                clues("OXYGEN", "HYDROGEN", "CARBON", "NITROGEN"),
                                clues("PROTON", "NEUTRON", "ELECTRON", "ATOM"),
                                clues("ACID", "BASE", "SALT", "SOLUTION"),
                                clues("MENDELEEV", "LABORATORY", "TEST TUBE", "REACTION")
                        ),
                        clues("ELEMENTS", "PARTICLES", "CHEMISTRY TERMS", "CHEMISTRY"),
                        "SCIENCE"
                ),
                association(
                        columns(
                                clues("PASSPORT", "SUITCASE", "AIRPLANE", "HOTEL"),
                                clues("PARIS", "ROME", "LONDON", "MADRID"),
                                clues("MAP", "COMPASS", "GUIDE", "TICKET"),
                                clues("SEA", "MOUNTAIN", "CITY", "SPA")
                        ),
                        clues("TRAVEL", "EUROPEAN CAPITALS", "NAVIGATION", "DESTINATIONS"),
                        "TOURISM"
                )
        );

        for (int i = 0; i < games.size(); i++) {
            db.collection("associationGames").document("association" + (i + 1)).set(games.get(i));
        }
    }

    public static void seedSkockoGames() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<SkockoGame> games = Arrays.asList(
                skocko(0, 3, 5, 1),
                skocko(2, 2, 4, 0),
                skocko(5, 1, 3, 3),
                skocko(4, 0, 1, 5),
                skocko(1, 5, 2, 4),
                skocko(3, 4, 0, 2),
                skocko(0, 0, 5, 3),
                skocko(2, 5, 1, 0),
                skocko(4, 4, 3, 1),
                skocko(5, 2, 0, 4)
        );

        for (int i = 0; i < games.size(); i++) {
            db.collection("skockoGames").document("skocko" + (i + 1)).set(games.get(i));
        }
    }

    private static AssociationGame association(List<List<String>> columns,
                                               List<String> columnSolutions,
                                               String finalSolution) {
        return new AssociationGame(
                columns.get(0),
                columns.get(1),
                columns.get(2),
                columns.get(3),
                columnSolutions,
                finalSolution
        );
    }

    private static List<List<String>> columns(List<String> a,
                                              List<String> b,
                                              List<String> c,
                                              List<String> d) {
        return Arrays.asList(a, b, c, d);
    }

    private static List<String> clues(String first, String second, String third, String fourth) {
        return Arrays.asList(first, second, third, fourth);
    }

    private static SkockoGame skocko(long first, long second, long third, long fourth) {
        return new SkockoGame(Arrays.asList(first, second, third, fourth));
    }
}
