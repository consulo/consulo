/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package consulo.versionControlSystem.impl.internal;

public interface BlockI {
    int getStart();

    int getEnd();

    int getVcsStart();

    int getVcsEnd();

    default boolean isEmpty() {
        return getStart() == getEnd() && getVcsStart() == getVcsEnd();
    }
}
