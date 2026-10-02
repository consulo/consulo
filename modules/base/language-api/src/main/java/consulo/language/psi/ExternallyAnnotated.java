// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.psi;

import consulo.document.util.TextRange;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ExternallyAnnotated {
    @Nullable TextRange getAnnotationRegion();
}
