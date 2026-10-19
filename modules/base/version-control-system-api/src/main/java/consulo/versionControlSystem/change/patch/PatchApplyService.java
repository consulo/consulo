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
package consulo.versionControlSystem.change.patch;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Applies patches to the files of a project the way Apply Patch does, without its dialog: files the patch does not
 * apply to cleanly open the merge of the patch.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface PatchApplyService {
    /**
     * @param baseDirectory the directory the paths of the patches are relative to
     * @param showSuccessNotification whether a notification tells that the patch was applied
     */
    @RequiredUIAccess
    CompletableFuture<ApplyPatchStatus> apply(VirtualFile baseDirectory, List<? extends FilePatch> patches, boolean showSuccessNotification);

    /**
     * Applies the hunks to a text in memory - a revision of a file rather than the file itself.
     *
     * @return the patched text, {@code null} when the hunks cannot be placed in the text
     */
    @Nullable AppliedPatch applyToText(CharSequence text, List<? extends PatchHunk> hunks);
}
