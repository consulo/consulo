// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.change.patch;

/**
 * A text with the hunks of a patch applied to it.
 */
public class AppliedPatch {
    private final String myPatchedText;
    private final ApplyPatchStatus myStatus;

    public AppliedPatch(String patchedText, ApplyPatchStatus status) {
        myPatchedText = patchedText;
        myStatus = status;
    }

    public String getPatchedText() {
        return myPatchedText;
    }

    public ApplyPatchStatus getStatus() {
        return myStatus;
    }
}
