// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

/**
 * @author gregsh
 */
public enum ReservedCellValue {
    NULL, DEFAULT, GENERATED, COMPUTED, UNSET;

    public String getDisplayName() {
        return switch (this) {
            case NULL -> "<null>";
            case DEFAULT -> "<default>";
            case GENERATED -> "<generated>";
            case COMPUTED -> "<computed>";
            case UNSET -> "<unset>";
        };
    }

    @Override
    public String toString() {
        return this == UNSET ? NULL.toString() : super.toString();
    }

    public String getSqlName() {
        return switch (this) {
            case NULL, UNSET -> "NULL";
            case DEFAULT -> "DEFAULT";
            case GENERATED -> "GENERATED";
            case COMPUTED -> "COMPUTED";
        };
    }
}
