package kr.go.namwon.seniorcenter.app.model;

import androidx.annotation.NonNull;

public class Center {
    private int id;
    private String name;
    private String region;

    public Center(int id, String name, String region) {
        this.id = id;
        this.name = name;
        this.region = region;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String toString() {
        return id == 0 ? name : region + " " + name + "경로당";
    }
}
