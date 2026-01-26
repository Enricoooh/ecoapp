package com.ecoapp.android.auth.models;

import android.content.Context;
import com.google.gson.annotations.SerializedName;

public class Quest {
    private final int id;
    private final String name;
    private final String description;
    private final String type; // alimentation, mobility...

    @SerializedName("max_progress") // -> per Retrofit e GSON
    private final int max_progress;
    @SerializedName("CO2_saved")
    private final double CO2_saved;
    @SerializedName("reward_points")
    private final int reward_points;
    @SerializedName("quest_image")
    private final String quest_image;
    @SerializedName("images_eu_goals")
    private final String[] images_eu_goals; // EU Sustainable Development Goals

    /* COSTRUTTORI */

    //Per non rischiare problemi con GSON
    public Quest() {
        this.id = -1;
        this.name = "";
        this.description = "";
        this.type = "";
        this.max_progress = 0;
        this.CO2_saved = 0.0;
        this.reward_points = 0;
        this.quest_image = "";
        this.images_eu_goals = new String[0];
    }

    public Quest(int id, String name, String description, String type, int max_progress, double CO2_saved, int reward_points, String quest_image, String[] images_eu_goals) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.max_progress = max_progress;
        this.CO2_saved = CO2_saved;
        this.reward_points = reward_points;
        this.quest_image = quest_image;
        this.images_eu_goals = images_eu_goals;
    }

    public Quest(LocalQuest lq) {
        this.id = lq.getId();
        this.name = lq.getName();
        this.description = lq.getDescription();
        this.type = lq.getType();
        this.max_progress = lq.getMaxProgress();
        this.CO2_saved = lq.getCO2Saved();
        this.reward_points = lq.getRewardPoints();
        this.quest_image = lq.getQuestImage();
        this.images_eu_goals = lq.getImagesEuGoals();
    }

    /* GETTERS */
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public int getMaxProgress() {
        return max_progress;
    }

    public double getCO2Saved() {
        return CO2_saved;
    }

    public int getRewardPoints() {
        return reward_points;
    }

    public String getQuestImage() {
        return quest_image;
    }

    /**
     * Trasforma il nome stringa dell'immagine (es. "ic_borraccia") nell'ID risorsa di Android.
     * @return L'ID della risorsa drawable, oppure 0 se non trovata.
     */
    public int getQuestImageResourceId(Context context) {
        if (quest_image == null || quest_image.isEmpty()) return 0;
        return context.getResources().getIdentifier(quest_image, "drawable", context.getPackageName());
    }

    public String[] getImagesEuGoals() {
        return images_eu_goals;
    }

    /**
     * Metodo helper per ottenere gli ID delle immagini degli obiettivi UE.
     */
    public int[] getEuGoalsResourceIds(Context context) {
        if (images_eu_goals == null) return new int[0];
        int[] resIds = new int[images_eu_goals.length];
        for (int i = 0; i < images_eu_goals.length; i++) {
            resIds[i] = context.getResources().getIdentifier(images_eu_goals[i], "drawable", context.getPackageName());
        }
        return resIds;
    }
}
