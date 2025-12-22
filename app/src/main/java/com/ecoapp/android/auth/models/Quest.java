package com.ecoapp.android.auth.models;

public class Quest {
    private final int id;
    private final String name;
    private final int progress;
    private final int imageResId;

    public Quest(int id, String name, int progress, int imageResId) {
        this.id = id;
        this.name = name;
        this.progress = progress;
        this.imageResId = imageResId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getProgress() { return progress; }
    public int getImageResId() { return imageResId; }
}
