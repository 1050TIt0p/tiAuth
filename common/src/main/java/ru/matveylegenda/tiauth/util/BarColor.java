package ru.matveylegenda.tiauth.util;

import lombok.Getter;

@Getter
public enum BarColor {
    PINK(0, "PINK"),
    BLUE(1, "BLUE"),
    RED(2, "RED"),
    GREEN(3, "GREEN"),
    YELLOW(4, "YELLOW"),
    PURPLE(5, "PURPLE"),
    WHITE(6, "WHITE");

    private final int id;
    private final String name;

    BarColor(int id, String name) {
        this.id = id;
        this.name = name;
    }
}