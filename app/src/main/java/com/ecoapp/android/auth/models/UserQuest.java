package com.ecoapp.android.auth.models;
import com.google.gson.annotations.SerializedName;

public class UserQuest {

    @SerializedName("questId") // -> per Retrofit e GSON
    private final int questId;

    @SerializedName("actual_progress")
    private int actual_progress;

    @SerializedName("times_completed")
    private int times_completed;

    @SerializedName("is_currently_active")
    private boolean is_currently_active;

    public UserQuest(int questId, int actual_progress, int times_completed, boolean is_currently_active) {
        this.questId = questId;
        this.actual_progress = actual_progress;
        this.times_completed = times_completed;
        this.is_currently_active = is_currently_active;
    }

    /* GETTERS */
    public int getQuestId() { return questId; }
    public int getActualProgress() { return actual_progress; }
    public int getTimesCompleted() { return times_completed; }
    public boolean isCurrentlyActive() { return is_currently_active; }

    // Metodo helper per sapere se è completata almeno una volta
    public boolean hasBeenCompleted() { return times_completed > 0; }

    /* SETTERS */

    public void setActualProgress(int actual_progress) {
        if(actual_progress >= 0)
            this.actual_progress = actual_progress;
    }

    public void setTimesCompleted(int times_completed) {
        this.times_completed = times_completed;
    }

    public void setCurrentlyActive(boolean currently_active) {
        is_currently_active = currently_active;
    }
}