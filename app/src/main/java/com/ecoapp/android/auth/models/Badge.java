package com.ecoapp.android.auth.models;

import com.example.ecoapp.R;

public class Badge {
    private int id;
    private String name;
    private String description;

    public Badge() {}

    public Badge(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getImageResId() {
        switch (id) {
            case 1: return R.drawable.badge01;
            case 2: return R.drawable.badge02;
            case 3: return R.drawable.badge03;
            case 4: return R.drawable.badge04;
            case 5: return R.drawable.badge05;
            case 6: return R.drawable.badge06;
            default: return R.drawable.badge01;
        }
    }
}
