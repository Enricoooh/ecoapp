package com.ecoapp.android.auth.models;

import java.util.List;

public class User {
    private String id;
    private String email;
    private String password;
    private String name;
    private String nickname;
    private String bio;
    private String urlImmagineProfilo;
    private String level;
    private int totalPoints;
    private double co2Saved;
    private int followerCount;
    private int followingCount;
    private List<String> friends;

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
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getUrlImmagineProfilo() { return urlImmagineProfilo; }
    public void setUrlImmagineProfilo(String urlImmagineProfilo) { this.urlImmagineProfilo = urlImmagineProfilo; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public double getCo2Saved() { return co2Saved; }
    public void setCo2Saved(double co2Saved) { this.co2Saved = co2Saved; }

    public int getFollowerCount() { return followerCount; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }

    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public List<String> getFriends() { return friends; }
    public void setFriends(List<String> friends) { this.friends = friends; }
}
