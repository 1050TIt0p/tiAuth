package ru.matveylegenda.tiauth.util;

import lombok.Getter;

@Getter
public enum BarStyle {
    SOLID(0),
    SEGMENTED_6(1),
    SEGMENTED_10(2),
    SEGMENTED_12(3),
    SEGMENTED_20(4);

    private final int id;

    BarStyle(int id) {
        this.id = id;
    }

}