package com.example.slagalica.models;

import java.util.List;

public class AssociationGame {

    private List<String> columnA;
    private List<String> columnB;
    private List<String> columnC;
    private List<String> columnD;
    private List<String> columnSolutions;
    private String finalSolution;

    public AssociationGame() {
    }

    public AssociationGame(List<String> columnA,
                           List<String> columnB,
                           List<String> columnC,
                           List<String> columnD,
                           List<String> columnSolutions,
                           String finalSolution) {
        this.columnA = columnA;
        this.columnB = columnB;
        this.columnC = columnC;
        this.columnD = columnD;
        this.columnSolutions = columnSolutions;
        this.finalSolution = finalSolution;
    }

    public List<String> getColumnA() {
        return columnA;
    }

    public void setColumnA(List<String> columnA) {
        this.columnA = columnA;
    }

    public List<String> getColumnB() {
        return columnB;
    }

    public void setColumnB(List<String> columnB) {
        this.columnB = columnB;
    }

    public List<String> getColumnC() {
        return columnC;
    }

    public void setColumnC(List<String> columnC) {
        this.columnC = columnC;
    }

    public List<String> getColumnD() {
        return columnD;
    }

    public void setColumnD(List<String> columnD) {
        this.columnD = columnD;
    }

    public List<String> getColumnSolutions() {
        return columnSolutions;
    }

    public void setColumnSolutions(List<String> columnSolutions) {
        this.columnSolutions = columnSolutions;
    }

    public String getFinalSolution() {
        return finalSolution;
    }

    public void setFinalSolution(String finalSolution) {
        this.finalSolution = finalSolution;
    }
}
