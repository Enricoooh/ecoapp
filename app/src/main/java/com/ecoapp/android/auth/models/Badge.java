package com.ecoapp.android.auth.models;

public class Badge {
    private final int id;
    private final String name;
    private final String description;
    private final int imageResId;

    public Badge(int id, String name, String description, int imageResId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageResId = imageResId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getImageResId() { return imageResId; }
}
