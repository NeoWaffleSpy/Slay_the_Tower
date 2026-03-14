package com.Team_Berry.Utils.TooltipInjector;

import com.Team_Berry.Utils.UtilsPlugin;

import java.awt.*;
import java.util.regex.Pattern;

public class StringFormatter {
    private StringBuilder sb = new StringBuilder();
    static Pattern HEX_COLOR = Pattern.compile("^#([A-Fa-f0-9]{6})$");
    public boolean isColorOpen = false;
    public boolean isItalicOpen = false;
    public boolean isBoldOpen = false;
    public boolean isUnderlineOpen = false;

    public StringFormatter() {}

    public StringFormatter color(Color color) {
        return setColor(String.format("#%06x", color.getRGB() & 0xFFFFFF));
    }

    public StringFormatter color(String hex) {
        if (!HEX_COLOR.matcher(hex).matches()) {
            UtilsPlugin.LOGGER.atWarning().log("Invalid hex color string: " + hex);
            return color(Color.WHITE);
        }
        return setColor(hex);
    }

    private StringFormatter setColor(String hex) {
        sb.append("<color is=\"").append(hex).append("\">");
        isColorOpen = true;
        return this;
    }

    public StringFormatter endColor() {
        sb.append("</color>");
        isColorOpen = false;
        return this;
    }

    public StringFormatter setItalic() { return setItalic(true); }
    public StringFormatter setItalic(boolean italic) {
        if (italic == isItalicOpen)
            return this;
        isItalicOpen = italic;
        if (italic)
            sb.append("<i>");
        else
            sb.append("</i>");
        return this;
    }

    public StringFormatter setBold() { return setBold(true); }
    public StringFormatter setBold(boolean bold) {
        if (bold == isBoldOpen)
            return this;
        isBoldOpen = bold;
        if (bold)
            sb.append("<b>");
        else
            sb.append("</b>");
        return this;
    }

    public StringFormatter setUnder() { return setUnder(true); }
    public StringFormatter setUnder(boolean under) {
        if (under == isUnderlineOpen)
            return this;
        isUnderlineOpen = under;
        if (under)
            sb.append("<u>");
        else
            sb.append("</u>");
        return this;
    }

    public boolean isColorOpen() {
        return isColorOpen;
    }

    public StringFormatter append(String txt) {
        sb.append(txt);
        return this;
    }

    public StringFormatter clear() {
        sb.setLength(0);
        return this;
    }

    public String toString() {
        if (isColorOpen) this.endColor();
        if (isItalicOpen) this.setItalic(false);
        if (isBoldOpen) this.setBold(false);
        if (isUnderlineOpen) this.setUnder(false);
        return sb.toString();
    }
}
