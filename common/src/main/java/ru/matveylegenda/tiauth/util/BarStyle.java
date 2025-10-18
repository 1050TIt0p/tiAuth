package ru.matveylegenda.tiauth.util;

import lombok.Getter;

@Getter
public enum BarStyle {
    SOLID(0, "PROGRESS"),
    SEGMENTED_6(1, "NOTCHED_6"),
    SEGMENTED_10(2, "NOTCHED_10"),
    SEGMENTED_12(3, "NOTCHED_12"),
    SEGMENTED_20(4, "NOTCHED_20");

    private final int id;
    private final String name;

    BarStyle(int id, String name) {
        this.id = id;
        this.name = name;
    }
}