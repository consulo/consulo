// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.patch.apply;

import consulo.versionControlSystem.change.patch.AppliedPatch;
import consulo.versionControlSystem.change.patch.ApplyPatchStatus;

public class AppliedSomehowPatch extends AppliedPatch {
    private final boolean myAppliedSomehow;

    public AppliedSomehowPatch(String text, ApplyPatchStatus status, boolean isAppliedSomehow) {
        super(text, status);
        myAppliedSomehow = isAppliedSomehow;
    }

    public boolean isAppliedSomehow() {
        return myAppliedSomehow;
    }
}
