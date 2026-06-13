package com.example.slagalica.models;

import java.util.List;

public class SkockoGame {

    private List<Long> secretCode;

    public SkockoGame() {
    }

    public SkockoGame(List<Long> secretCode) {
        this.secretCode = secretCode;
    }

    public List<Long> getSecretCode() {
        return secretCode;
    }

    public void setSecretCode(List<Long> secretCode) {
        this.secretCode = secretCode;
    }
}
