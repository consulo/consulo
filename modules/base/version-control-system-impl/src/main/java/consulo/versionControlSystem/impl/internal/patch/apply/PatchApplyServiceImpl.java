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

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.change.patch.AppliedPatch;
import consulo.versionControlSystem.change.patch.ApplyPatchStatus;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.patch.PatchApplyService;
import consulo.versionControlSystem.change.patch.PatchHunk;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@Singleton
@ServiceImpl
public class PatchApplyServiceImpl implements PatchApplyService {
    private final Project myProject;

    @Inject
    public PatchApplyServiceImpl(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<ApplyPatchStatus> apply(VirtualFile baseDirectory,
                                                     List<? extends FilePatch> patches,
                                                     boolean showSuccessNotification) {
        List<FilePatch> filePatches = new ArrayList<>(patches);
        return new PatchApplier<>(myProject, baseDirectory, filePatches, (LocalChangeList) null, null, null)
            .execute(showSuccessNotification, false);
    }

    @Override
    public @Nullable AppliedPatch applyToText(CharSequence text, List<? extends PatchHunk> hunks) {
        return GenericPatchApplier.apply(text, hunks);
    }
}
