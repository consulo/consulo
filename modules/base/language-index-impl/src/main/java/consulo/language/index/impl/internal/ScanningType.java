// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import java.util.List;

/**
 * isFull - if the whole project was rescanned (instead of a part of it)
 */
public enum ScanningType {
    /**
     * Full project rescan forced by user via Repair IDE action
     */
    FULL_FORCED(true),

    /**
     * Full project rescan on project open
     */
    FULL_ON_PROJECT_OPEN(true),

    /**
     * Full project rescan on index restart.
     * Index restart happens in two cases:
     * 1. When a language plugin is turned on/off (see FileBasedIndexTumbler)
     * 2. In tests (see usages of FileBasedIndexTumbler.turnOff)
     */
    FULL_ON_INDEX_RESTART(true),

    /**
     * Full project rescan requested by some code
     */
    FULL(true),

    /**
     * Partial rescan forced by user via Repair IDE action on a limited scope (not full project)
     */
    PARTIAL_FORCED(false),

    /**
     * Full scanning on project open was skipped, and only dirty files from the last IDE session are scanned
     */
    PARTIAL_ON_PROJECT_OPEN(false),

    /**
     * Partial project rescan on index restart.
     * Index restart happens in two cases:
     * 1. When a language plugin is turned on/off (see FileBasedIndexTumbler)
     * 2. In tests (see usages of FileBasedIndexTumbler.turnOff)
     * <p>
     * The first case (when a language plugin is turned on/off) requires full rescan
     * because we don't know which files need to be indexed, therefore, this type can only appear in tests.
     */
    PARTIAL_ON_INDEX_RESTART(false),

    /**
     * Partial project rescan requested by some code
     */
    PARTIAL(false),

    /**
     * Some files were considered changed and therefore rescanned
     */
    REFRESH(false);

    private final boolean myFull;

    ScanningType(boolean full) {
        myFull = full;
    }

    public boolean isFull() {
        return myFull;
    }

    public static ScanningType merge(ScanningType first, ScanningType second) {
        return returnFirstFound(first, second);
    }

    private static ScanningType returnFirstFound(ScanningType first, ScanningType second) {
        List<ScanningType> types =
            List.of(FULL_FORCED, FULL_ON_PROJECT_OPEN, FULL, PARTIAL_FORCED, PARTIAL_ON_PROJECT_OPEN, PARTIAL, REFRESH);
        for (ScanningType type : types) {
            if (first == type || second == type) {
                return type;
            }
        }
        throw new IllegalStateException("Unexpected ScanningType " + first + " " + second);
    }
}
