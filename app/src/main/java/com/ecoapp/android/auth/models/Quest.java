package com.ecoapp.android.auth.models;

public class Quest {
    private final int id;
    private final String name;
    private String description;
    private String type; // alimentation, mobility...
    private int actual_progress;
    private int max_progress;
    private int imageResId;
    private int[] images_eu_goals; // EU Sustainable Development Goals
    private int reward_points;

    /* COSTRUTTORI */
    public Quest(int id, String name, String type, int max_progress, int imageResId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.max_progress = max_progress;
        this.imageResId = imageResId;
    }

    public Quest(int id, String name, String type, int actual_progress, int max_progress, int imageResId,
                 String description, int[] images_eu_goals, int reward_points) {
        this(id, name, type, max_progress, imageResId);
        this.actual_progress = actual_progress;
        this.description = description;
        this.images_eu_goals = images_eu_goals;
        this.reward_points = reward_points;
    }

    /* GETTERS */
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getActualProgress() { return actual_progress; }
    public int getMaxProgress() { return max_progress; }
    public int getImageResId() { return imageResId; }
    public String getDescription() { return description; }
    public int[] getImages_eu_goals() { return images_eu_goals; }
    public int getReward_points() { return reward_points; }

    /* SETTERS */
    public void setType(String type) {
        if (type != null) this.type = type;
    }

    public void setActualProgress(int actual_progress) {
        if (actual_progress >= 0) this.actual_progress = actual_progress;
    }

    public void setMaxProgress(int max_progress) {
        if (max_progress >= 0) this.max_progress = max_progress;
    }

    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

    public void setDescription(String description) {
        if (description != null) this.description = description;
    }

    public void setImages_eu_goals(int[] images_eu_goals) { this.images_eu_goals = images_eu_goals; }

    public void setReward_points(int reward_points) {
        if (reward_points >= 0) this.reward_points = reward_points;
    }
}
