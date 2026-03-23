// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

/**
 * Describes the position of entry relative to target range
 */
public enum CodeVisionAnchorKind {
    /**
     * Above the target range
     */
    Top("codeLens.entry.position.top"),

    /**
     * After end of line with target range
     */
    Right("codeLens.entry.position.right"),

    /**
     * On the same line as target range, near the scrollbar
     */
    NearScroll("codeLens.entry.position.nearScroll"),

    /**
     * In any empty space near the target range
     */
    EmptySpace("codeLens.entry.position.emptySpace"),

    /**
     * Use the global default value from settings
     */
    Default("codeLens.entry.position.default");

    private final String key;

    CodeVisionAnchorKind(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
