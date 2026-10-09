package com.goosker.compass.client.hud;

public enum MarkerType {
    NORMAL(10),
    SERVER(20),
    WAYSTONE(30),
    SPAWN(40),
    DEATH(50);

    private final int priority;

    MarkerType(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
