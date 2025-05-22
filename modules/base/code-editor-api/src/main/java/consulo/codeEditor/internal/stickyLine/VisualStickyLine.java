// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.internal.stickyLine;

import consulo.document.util.TextRange;

public class VisualStickyLine implements StickyLine {
    private final StickyLine origin;
    private final int primaryVisualLine;
    private final int scopeVisualLine;
    private int yLocation;

    public VisualStickyLine(StickyLine origin, int primaryVisualLine, int scopeVisualLine) {
        this(origin, primaryVisualLine, scopeVisualLine, 0);
    }

    public VisualStickyLine(StickyLine origin, int primaryVisualLine, int scopeVisualLine, int yLocation) {
        this.origin = origin;
        this.primaryVisualLine = primaryVisualLine;
        this.scopeVisualLine = scopeVisualLine;
        this.yLocation = yLocation;
    }

    @Override
    public int primaryLine() {
        return primaryVisualLine;
    }

    @Override
    public int scopeLine() {
        return scopeVisualLine;
    }

    @Override
    public int navigateOffset() {
        return origin.navigateOffset();
    }

    @Override
    public TextRange textRange() {
        return origin.textRange();
    }

    @Override
    public String debugText() {
        return origin.debugText();
    }

    @Override
    public int compareTo(StickyLine other) {
        int cmp = Integer.compare(primaryLine(), other.primaryLine());
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(other.scopeLine(), scopeLine());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VisualStickyLine)) return false;
        VisualStickyLine other = (VisualStickyLine) obj;
        return primaryVisualLine == other.primaryVisualLine &&
            scopeVisualLine == other.scopeVisualLine;
    }

    @Override
    public int hashCode() {
        int result = primaryVisualLine;
        result = 31 * result + scopeVisualLine;
        return result;
    }

    @Override
    public String toString() {
        String text = debugText();
        return (text != null ? text : "") + "(" + primaryVisualLine + ", " + scopeVisualLine + ", " + yLocation + ")";
    }

    public int getYLocation() {
        return yLocation;
    }

    public void setYLocation(int yLocation) {
        this.yLocation = yLocation;
    }
}
