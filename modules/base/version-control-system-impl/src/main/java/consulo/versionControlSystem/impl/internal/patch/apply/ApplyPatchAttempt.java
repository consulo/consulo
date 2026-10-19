/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.versionControlSystem.impl.internal.patch.apply;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.change.patch.ApplyPatchStatus;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public final class ApplyPatchAttempt {
    private final VirtualFile myFile;
    private final ApplyFilePatchBase<?> myPatch;
    private final @Nullable ApplyPatchStatus myStatus;
    private final @Nullable Supplier<CompletableFuture<ApplyPatchStatus>> myMerge;

    private ApplyPatchAttempt(
        VirtualFile file,
        ApplyFilePatchBase<?> patch,
        @Nullable ApplyPatchStatus status,
        @Nullable Supplier<CompletableFuture<ApplyPatchStatus>> merge
    ) {
        myFile = file;
        myPatch = patch;
        myStatus = status;
        myMerge = merge;
    }

    public static ApplyPatchAttempt done(VirtualFile file, ApplyFilePatchBase<?> patch, @Nullable ApplyPatchStatus status) {
        return new ApplyPatchAttempt(file, patch, status, null);
    }

    public static ApplyPatchAttempt merge(
        VirtualFile file,
        ApplyFilePatchBase<?> patch,
        Supplier<CompletableFuture<ApplyPatchStatus>> merge
    ) {
        return new ApplyPatchAttempt(file, patch, null, merge);
    }

    public VirtualFile getFile() {
        return myFile;
    }

    public ApplyFilePatchBase<?> getPatch() {
        return myPatch;
    }

    public boolean isMergeNeeded() {
        return myMerge != null;
    }

    public @Nullable ApplyPatchStatus getStatus() {
        return myStatus;
    }

    @RequiredUIAccess
    public CompletableFuture<ApplyPatchStatus> runMerge() {
        Supplier<CompletableFuture<ApplyPatchStatus>> merge = myMerge;
        if (merge == null) {
            return CompletableFuture.completedFuture(myStatus);
        }
        return merge.get();
    }
}
