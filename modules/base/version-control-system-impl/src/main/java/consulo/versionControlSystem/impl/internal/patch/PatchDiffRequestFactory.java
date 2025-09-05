/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.patch;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffRequestFactory;
import consulo.diff.InvalidDiffRequestException;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.content.DocumentContent;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.document.Document;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.diff.ChangeDiffRequestProducer;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.impl.internal.patch.apply.AppliedTextPatch;
import consulo.versionControlSystem.impl.internal.patch.apply.ApplyPatchForBaseRevisionTexts;
import consulo.versionControlSystem.impl.internal.patch.apply.GenericPatchApplier;
import consulo.versionControlSystem.impl.internal.patch.tool.ApplyPatchDiffRequest;
import consulo.versionControlSystem.impl.internal.patch.tool.ApplyPatchMergeRequest;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PatchDiffRequestFactory {
    @Nonnull
    public static DiffRequest createDiffRequest(
        @Nullable Project project,
        @Nonnull Change change,
        @Nonnull String name,
        @Nonnull UserDataHolder context,
        @Nonnull ProgressIndicator indicator
    ) throws DiffRequestProducerException {
        ChangeDiffRequestProducer proxyProducer = ChangeDiffRequestProducer.create(project, change);
        if (proxyProducer == null) {
            throw new DiffRequestProducerException("Can't show diff for '" + name + "'");
        }
        return proxyProducer.process(context, indicator);
    }

    @Nonnull
    @RequiredUIAccess
    public static DiffRequest createConflictDiffRequest(
        @Nullable Project project,
        @Nullable VirtualFile file,
        @Nonnull TextFilePatch patch,
        @Nonnull String afterTitle,
        @Nonnull Supplier<ApplyPatchForBaseRevisionTexts> textsGetter,
        @Nonnull String name,
        @Nonnull UserDataHolder context,
        @Nonnull ProgressIndicator indicator
    ) throws DiffRequestProducerException {
        if (file == null) {
            throw new DiffRequestProducerException("Can't show diff for '" + name + "'");
        }
        if (file.getFileType().isBinary()) {
            throw new DiffRequestProducerException("Can't show diff for binary file '" + name + "'");
        }

        SimpleReference<ApplyPatchForBaseRevisionTexts> textsRef = new SimpleReference<>();
        Application.get().invokeAndWait(() -> textsRef.set(textsGetter.get()), indicator.getModalityState());
        ApplyPatchForBaseRevisionTexts texts = textsRef.get();

        if (texts.getLocal() == null) {
            throw new DiffRequestProducerException("Can't show diff for '" + file.getPresentableUrl() + "'");
        }

        if (texts.getBase() == null) {
            String localContent = texts.getLocal().toString();

            GenericPatchApplier applier = new GenericPatchApplier(localContent, patch.getHunks());
            applier.execute();

            AppliedTextPatch appliedTextPatch = AppliedTextPatch.create(applier.getAppliedInfo());
            return createBadDiffRequest(project, file, localContent, appliedTextPatch, null, null, null, null);
        }
        else {
            String localContent = texts.getLocal().toString();
            String baseContent = texts.getBase().toString();
            String patchedContent = texts.getPatched();

            return createDiffRequest(
                project,
                file,
                ContainerUtil.list(localContent, baseContent, patchedContent),
                null,
                ContainerUtil.list("Current Version", "Base Version", afterTitle)
            );
        }
    }

    @Nonnull
    public static DiffRequest createDiffRequest(
        @Nullable Project project,
        @Nullable VirtualFile file,
        @Nonnull List<String> contents,
        @Nullable String windowTitle,
        @Nonnull List<String> titles
    ) {
        assert contents.size() == 3;
        assert titles.size() == 3;

        if (windowTitle == null) {
            windowTitle = getPatchTitle(file);
        }

        String localTitle = StringUtil.notNullize(titles.get(0), VcsLocalize.patchApplyConflictLocalVersion().get());
        String baseTitle = StringUtil.notNullize(titles.get(1), "Base Version");
        String patchedTitle = StringUtil.notNullize(titles.get(2), VcsLocalize.patchApplyConflictPatchedVersion().get());

        FileType fileType = file != null ? file.getFileType() : null;

        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        DocumentContent localContent = file != null ? contentFactory.createDocument(project, file) : null;
        if (localContent == null) {
            localContent = contentFactory.create(contents.get(0), fileType);
        }
        DocumentContent baseContent = contentFactory.create(contents.get(1), fileType);
        DocumentContent patchedContent = contentFactory.create(contents.get(2), fileType);

        return new SimpleDiffRequest(windowTitle, localContent, baseContent, patchedContent,
            localTitle, baseTitle, patchedTitle
        );
    }

    @Nonnull
    public static DiffRequest createBadDiffRequest(
        @Nullable Project project,
        @Nonnull VirtualFile file,
        @Nonnull String localContent,
        @Nonnull AppliedTextPatch textPatch,
        @Nullable String windowTitle,
        @Nullable String localTitle,
        @Nullable String resultTitle,
        @Nullable String patchTitle
    ) {
        if (windowTitle == null) {
            windowTitle = getBadPatchTitle(file);
        }
        if (localTitle == null) {
            localTitle = VcsLocalize.patchApplyConflictLocalVersion().get();
        }
        if (resultTitle == null) {
            resultTitle = VcsLocalize.patchApplyConflictPatchedSomehowVersion().get();
        }
        if (patchTitle == null) {
            patchTitle = VcsLocalize.patchApplyConflictPatch().get();
        }

        DocumentContent resultContent = DiffContentFactory.getInstance().createDocument(project, file);
        if (resultContent == null) {
            resultContent = DiffContentFactory.getInstance().create(localContent, file);
        }
        return new ApplyPatchDiffRequest(
            resultContent,
            textPatch,
            localContent,
            windowTitle,
            localTitle,
            resultTitle,
            patchTitle
        );
    }

    @Nonnull
    public static MergeRequest createMergeRequest(
        @Nullable Project project,
        @Nonnull Document document,
        @Nonnull VirtualFile file,
        @Nonnull String baseContent,
        @Nonnull String localContent,
        @Nonnull String patchedContent,
        @Nullable Consumer<MergeResult> callback
    ) throws InvalidDiffRequestException {
        List<String> titles = ContainerUtil.list(null, null, null);
        List<String> contents = ContainerUtil.list(localContent, baseContent, patchedContent);

        return createMergeRequest(project, document, file, contents, null, titles, callback);
    }

    @Nonnull
    public static MergeRequest createBadMergeRequest(
        @Nullable Project project,
        @Nonnull Document document,
        @Nonnull VirtualFile file,
        @Nonnull String localContent,
        @Nonnull AppliedTextPatch textPatch,
        @Nullable Consumer<MergeResult> callback
    ) throws InvalidDiffRequestException {
        return createBadMergeRequest(
            project,
            document,
            file,
            localContent,
            textPatch,
            null,
            null,
            null,
            null,
            callback
        );
    }

    @Nonnull
    public static MergeRequest createMergeRequest(
        @Nullable Project project,
        @Nonnull Document document,
        @Nullable VirtualFile file,
        @Nonnull List<String> contents,
        @Nullable String windowTitle,
        @Nonnull List<String> titles,
        @Nullable Consumer<MergeResult> callback
    ) throws InvalidDiffRequestException {
        assert contents.size() == 3;
        assert titles.size() == 3;

        if (windowTitle == null) {
            windowTitle = getPatchTitle(file);
        }

        String localTitle = StringUtil.notNullize(titles.get(0), VcsLocalize.patchApplyConflictLocalVersion().get());
        String baseTitle = StringUtil.notNullize(titles.get(1), VcsLocalize.patchApplyConflictMergedVersion().get());
        String patchedTitle = StringUtil.notNullize(titles.get(2), VcsLocalize.patchApplyConflictPatchedVersion().get());

        List<String> actualTitles = ContainerUtil.list(localTitle, baseTitle, patchedTitle);

        FileType fileType = file != null ? file.getFileType() : null;
        return DiffRequestFactory.getInstance()
            .createMergeRequest(project, fileType, document, contents, windowTitle, actualTitles, callback);
    }

    @Nonnull
    public static MergeRequest createBadMergeRequest(
        @Nullable Project project,
        @Nonnull Document document,
        @Nullable VirtualFile file,
        @Nonnull String localContent,
        @Nonnull AppliedTextPatch textPatch,
        @Nullable String windowTitle,
        @Nullable String localTitle,
        @Nullable String resultTitle,
        @Nullable String patchTitle,
        @Nullable Consumer<MergeResult> callback
    ) throws InvalidDiffRequestException {
        if (!DiffImplUtil.canMakeWritable(document)) {
            throw new InvalidDiffRequestException(
                "Output is read only" + (file != null ? " : '" + file.getPresentableUrl() + "'" : "")
            );
        }

        if (windowTitle == null) {
            windowTitle = getBadPatchTitle(file);
        }
        if (localTitle == null) {
            localTitle = VcsLocalize.patchApplyConflictLocalVersion().get();
        }
        if (resultTitle == null) {
            resultTitle = VcsLocalize.patchApplyConflictPatchedSomehowVersion().get();
        }
        if (patchTitle == null) {
            patchTitle = VcsLocalize.patchApplyConflictPatch().get();
        }

        DocumentContent resultContent = DiffContentFactory.getInstance().create(project, document, file);
        return new ApplyPatchMergeRequest(
            project,
            resultContent,
            textPatch,
            localContent,
            windowTitle,
            localTitle,
            resultTitle,
            patchTitle,
            callback
        );
    }

    @Nonnull
    private static String getPatchTitle(@Nullable VirtualFile file) {
        if (file != null) {
            return VcsLocalize.patchApplyConflictTitle(getPresentablePath(file)).get();
        }
        else {
            return "Patch Conflict";
        }
    }

    @Nonnull
    private static String getBadPatchTitle(@Nullable VirtualFile file) {
        return file != null ? "Result of Patch Apply to " + getPresentablePath(file) : "Result of Patch Apply";
    }

    @Nonnull
    private static String getPresentablePath(@Nonnull VirtualFile file) {
        String fullPath = file.getParent() == null ? file.getPath() : file.getParent().getPath();
        return file.getName() + " (" + fullPath + ")";
    }
}

