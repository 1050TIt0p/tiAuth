package ru.matveylegenda.tiauth.util;

import lombok.experimental.UtilityClass;
import ru.matveylegenda.tiauth.util.colorizer.Colorizer;
import ru.matveylegenda.tiauth.util.colorizer.Serializer;
import ru.matveylegenda.tiauth.util.colorizer.impl.LegacyColorizer;
import ru.matveylegenda.tiauth.util.colorizer.impl.MiniMessageColorizer;

@UtilityClass
public class Utils {

    public Colorizer COLORIZER;

    public void initializeColorizer(Serializer serializer) {
        COLORIZER = switch (serializer) {
            case LEGACY -> new LegacyColorizer();
            case MINIMESSAGE -> new MiniMessageColorizer();
        };
    }

    public final char COLOR_CHAR = '§';

    public String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        final char[] b = textToTranslate.toCharArray();

        for (int i = 0, length = b.length - 1; i < length; i++) {
            if (b[i] == altColorChar && isValidColorCharacter(b[i + 1])) {
                b[i++] = COLOR_CHAR;
                b[i] |= 0x20;
            }
        }

        return new String(b);
    }

    private boolean isValidColorCharacter(char c) {
        return switch (c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D',
                 'E', 'F', 'r', 'R', 'k', 'K', 'l', 'L', 'm', 'M', 'n', 'N', 'o', 'O', 'x', 'X' -> true;
            default -> false;
        };
    }
}
