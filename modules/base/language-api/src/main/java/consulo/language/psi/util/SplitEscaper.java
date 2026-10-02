// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.psi.util;

public interface SplitEscaper {
    SplitEscaper ACCEPT_ALL = (lastSplit, currentPosition) -> true;

    boolean filter(int lastSplit, int currentPosition);
}
