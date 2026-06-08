package com.example.slagalica.models;

public class PlayerStatistics {

    private int quizGamesPlayed;
    private int quizCorrectAnswers;
    private int quizTotalQuestions;

    private int matchingGamesPlayed;
    private int matchingCorrectMatches;
    private int matchingTotalMatches;

    public PlayerStatistics() {
    }

    public int getQuizGamesPlayed() {
        return quizGamesPlayed;
    }

    public void setQuizGamesPlayed(int quizGamesPlayed) {
        this.quizGamesPlayed = quizGamesPlayed;
    }

    public int getQuizCorrectAnswers() {
        return quizCorrectAnswers;
    }

    public void setQuizCorrectAnswers(int quizCorrectAnswers) {
        this.quizCorrectAnswers = quizCorrectAnswers;
    }

    public int getQuizTotalQuestions() {
        return quizTotalQuestions;
    }

    public void setQuizTotalQuestions(int quizTotalQuestions) {
        this.quizTotalQuestions = quizTotalQuestions;
    }

    public int getMatchingGamesPlayed() {
        return matchingGamesPlayed;
    }

    public void setMatchingGamesPlayed(int matchingGamesPlayed) {
        this.matchingGamesPlayed = matchingGamesPlayed;
    }

    public int getMatchingCorrectMatches() {
        return matchingCorrectMatches;
    }

    public void setMatchingCorrectMatches(int matchingCorrectMatches) {
        this.matchingCorrectMatches = matchingCorrectMatches;
    }

    public int getMatchingTotalMatches() {
        return matchingTotalMatches;
    }

    public void setMatchingTotalMatches(int matchingTotalMatches) {
        this.matchingTotalMatches = matchingTotalMatches;
    }
}