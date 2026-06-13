package com.example.slagalica.models;

public class MatchingPair {
    private String left;
    private String right;

    public MatchingPair() {
    }

    public MatchingPair(String left, String right) {
        this.left = left;
        this.right = right;
    }

    public String getLeft() {
        return left;
    }

    public String getRight() {
        return right;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public void setRight(String right) {
        this.right = right;
    }
}