package com.ecoapp.android.auth.models;

public class User {
    private String id;
    private String email;
    private String name;
    private String level;
    private int totalPoints;
    private double co2Saved;

    // Constructors
    public User() {}

    public User(String id, String email, String name, String level, int totalPoints, double co2Saved) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.level = level;
        this.totalPoints = totalPoints;
        this.co2Saved = co2Saved;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public double getCo2Saved() {
        return co2Saved;
    }

    public void setCo2Saved(double co2Saved) {
        this.co2Saved = co2Saved;
    }
}
