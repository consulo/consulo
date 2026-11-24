/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.patch;

import consulo.component.extension.ExtensionPoint;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.change.patch.*;
import consulo.versionControlSystem.impl.internal.patch.apply.PatchApplier;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ApplyPatchDefaultExecutor implements ApplyPatchExecutor<AbstractFilePatchInProgress> {
    protected final Project myProject;

    public ApplyPatchDefaultExecutor(Project project) {
        myProject = project;
    }

    @Override
    public String getName() {
        // not used
        return null;
    }

    @Override
    @RequiredUIAccess
    public void apply(
        List<FilePatch> remaining,
        MultiMap<VirtualFile, AbstractFilePatchInProgress> patchGroupsToApply,
        @Nullable LocalChangeList localList,
        @Nullable String fileName,
        @Nullable ThrowableSupplier<Map<String, Map<String, CharSequence>>, PatchSyntaxException> additionalInfo
    ) {
        CommitContext commitContext = new CommitContext();
        applyAdditionalInfoBefore(myProject, additionalInfo, commitContext);
        Collection<PatchApplier> appliers = getPatchAppliers(patchGroupsToApply, localList, commitContext);
        executeAndApplyAdditionalInfo(localList, additionalInfo, commitContext, appliers);
    }

    @RequiredUIAccess
    protected CompletableFuture<ApplyPatchStatus> executeAndApplyAdditionalInfo(
        @Nullable LocalChangeList localList,
        @Nullable ThrowableSupplier<Map<String, Map<String, CharSequence>>, PatchSyntaxException> additionalInfo,
        CommitContext commitContext,
        Collection<PatchApplier> appliers
    ) {
        UIAccess uiAccess = UIAccess.current();
        return PatchApplier.executePatchGroup(appliers, localList).whenCompleteAsync(
            (applyPatchStatus, error) -> {
                if (error == null && applyPatchStatus != ApplyPatchStatus.ABORT) {
                    applyAdditionalInfo(myProject, additionalInfo, commitContext);
                }
            },
            uiAccess
        );
    }

    protected Collection<PatchApplier> getPatchAppliers(
        MultiMap<VirtualFile, AbstractFilePatchInProgress> patchGroups,
        @Nullable LocalChangeList localList,
        CommitContext commitContext
    ) {
        Collection<PatchApplier> appliers = new LinkedList<>();
        for (VirtualFile base : patchGroups.keySet()) {
            appliers.add(new PatchApplier<BinaryFilePatch>(
                myProject,
                base,
                ContainerUtil.map(patchGroups.get(base), AbstractFilePatchInProgress::getPatch),
                localList,
                null,
                commitContext
            ));
        }
        return appliers;
    }

    public static void applyAdditionalInfoBefore(
        Project project,
        @Nullable ThrowableSupplier<Map<String, Map<String, CharSequence>>, PatchSyntaxException> additionalInfo,
        CommitContext commitContext
    ) {
        applyAdditionalInfoImpl(
            project,
            additionalInfo,
            commitContext,
            infoGroup -> infoGroup.myPatchEP.consumeContentBeforePatchApplied(
                infoGroup.myPath,
                infoGroup.myContent,
                infoGroup.myCommitContext
            )
        );
    }

    private static void applyAdditionalInfo(
        Project project,
        @Nullable ThrowableSupplier<Map<String, Map<String, CharSequence>>, PatchSyntaxException> additionalInfo,
        CommitContext commitContext
    ) {
        applyAdditionalInfoImpl(
            project,
            additionalInfo,
            commitContext,
            infoGroup -> infoGroup.myPatchEP.consumeContent(infoGroup.myPath, infoGroup.myContent, infoGroup.myCommitContext)
        );
    }

    private static void applyAdditionalInfoImpl(
        Project project,
        @Nullable ThrowableSupplier<Map<String, Map<String, CharSequence>>, PatchSyntaxException> additionalInfo,
        CommitContext commitContext,
        Consumer<InfoGroup> worker
    ) {
        ExtensionPoint<PatchEP> extensionPoint = project.getExtensionPoint(PatchEP.class);
        if (!extensionPoint.hasAnyExtensions()) {
            return;
        }
        if (additionalInfo != null) {
            try {
                for (Map.Entry<String, Map<String, CharSequence>> entry : additionalInfo.get().entrySet()) {
                    String path = entry.getKey();
                    Map<String, CharSequence> innerMap = entry.getValue();

                    extensionPoint.forEach(extension -> {
                        CharSequence charSequence = innerMap.get(extension.getName());
                        if (charSequence != null) {
                            worker.accept(new InfoGroup(extension, path, charSequence, commitContext));
                        }
                    });
                }
            }
            catch (PatchSyntaxException e) {
                VcsBalloonProblemNotifier.showOverChangesView(
                    project,
                    "Can not apply additional patch info: " + e.getMessage(),
                    NotificationType.ERROR
                );
            }
        }
    }

    private static class InfoGroup {
        private final PatchEP myPatchEP;
        private final String myPath;
        private final CharSequence myContent;
        private final CommitContext myCommitContext;

        private InfoGroup(PatchEP patchEP, String path, CharSequence content, CommitContext commitContext) {
            myPatchEP = patchEP;
            myPath = path;
            myContent = content;
            myCommitContext = commitContext;
        }
    }

    public static Set<String> pathsFromGroups(MultiMap<VirtualFile, AbstractFilePatchInProgress> patchGroups) {
        Set<String> selectedPaths = new HashSet<>();
        Collection<? extends AbstractFilePatchInProgress> values = patchGroups.values();
        for (AbstractFilePatchInProgress value : values) {
            String path = value.getPatch().getBeforeName() == null ? value.getPatch().getAfterName() : value.getPatch().getBeforeName();
            selectedPaths.add(path);
        }
        return selectedPaths;
    }
}
