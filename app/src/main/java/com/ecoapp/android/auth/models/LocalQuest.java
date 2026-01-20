//Fusion between Quest.java and UserQuest.java, used for making maps
package com.ecoapp.android.auth.models;

import com.google.gson.annotations.SerializedName;

public class LocalQuest extends Quest{
    //USERQUEST PART
    @SerializedName("actual_progress")
    private int actual_progress;

    @SerializedName("times_completed")
    private int times_completed;

    @SerializedName("is_currently_active")
    private boolean is_currently_active;

    //Per non rischiare problemi con GSON
    public LocalQuest() {
        super();
    }

    public LocalQuest(int id, String name, String description, String type, int max_progress, double CO2_saved, int reward_points, String quest_image, String[] images_eu_goals, int actual_progress, int times_completed, boolean is_currently_active) {
        //QUEST PART
        super(id, name, description, type, max_progress, CO2_saved, reward_points, quest_image, images_eu_goals);

        this.actual_progress = actual_progress;
        this.times_completed = times_completed;
        this.is_currently_active = is_currently_active;
    }

    public LocalQuest(Quest q, UserQuest uq){
        //QUEST PART
        super(q.getId(), q.getName(), q.getDescription(), q.getType(), q.getMaxProgress(),
                q.getCO2Saved(), q.getRewardPoints(), q.getQuestImage(), q.getImagesEuGoals());

        //USERQUEST PART
        this.actual_progress = uq.getActualProgress();
        this.times_completed = uq.getTimesCompleted();
        this.is_currently_active = uq.isCurrentlyActive();
    }

    public LocalQuest(Quest q){
        //QUEST PART
        super(q.getId(), q.getName(), q.getDescription(), q.getType(), q.getMaxProgress(),
                q.getCO2Saved(), q.getRewardPoints(), q.getQuestImage(), q.getImagesEuGoals());
    }

    public LocalQuest(Quest q, int actual_progress, int times_completed, boolean is_currently_active){
        //QUEST PART
        super(q.getId(), q.getName(), q.getDescription(), q.getType(), q.getMaxProgress(),
                q.getCO2Saved(), q.getRewardPoints(), q.getQuestImage(), q.getImagesEuGoals());

        //USERQUEST PART
        this.actual_progress = actual_progress;
        this.times_completed = times_completed;
        this.is_currently_active = is_currently_active;
    }

    public LocalQuest(LocalQuest lq){
        //QUEST PART
        super(lq.getId(), lq.getName(), lq.getDescription(), lq.getType(), lq.getMaxProgress(),
                lq.getCO2Saved(), lq.getRewardPoints(), lq.getQuestImage(), lq.getImagesEuGoals());

        //USERQUEST PART
        this.actual_progress = lq.getActualProgress();
        this.times_completed = lq.getTimesCompleted();
        this.is_currently_active = lq.isCurrentlyActive();
    }

    /*GETTERS USERQUEST*/
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
