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
package consulo.diff.impl.internal.request;

import consulo.diff.content.DiffContent;
import consulo.diff.content.FileContent;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.merge.BinaryMergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.util.ThreeSide;
import consulo.language.editor.WriteCommandAction;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class BinaryMergeRequestImpl extends BinaryMergeRequest {
    private static final Logger LOG = Logger.getInstance(BinaryMergeRequestImpl.class);

    @Nullable
    private final Project myProject;
    @Nonnull
    private final FileContent myFile;
    @Nonnull
    private final List<DiffContent> myContents;

    @Nonnull
    private final List<byte[]> myByteContents;
    @Nonnull
    private final byte[] myOriginalContent;

    @Nullable
    private final String myTitle;
    @Nonnull
    private final List<String> myTitles;

    @Nullable
    private final Consumer<MergeResult> myApplyCallback;

    public BinaryMergeRequestImpl(
        @Nullable Project project,
        @Nonnull FileContent file,
        @Nonnull byte[] originalContent,
        @Nonnull List<DiffContent> contents,
        @Nonnull List<byte[]> byteContents,
        @Nullable String title,
        @Nonnull List<String> contentTitles,
        @Nullable Consumer<MergeResult> applyCallback
    ) {
        assert byteContents.size() == 3;
        assert contents.size() == 3;
        assert contentTitles.size() == 3;

        myProject = project;
        myFile = file;
        myOriginalContent = originalContent;

        myByteContents = byteContents;
        myContents = contents;
        myTitle = title;
        myTitles = contentTitles;

        myApplyCallback = applyCallback;
    }

    @Nonnull
    @Override
    public FileContent getOutputContent() {
        return myFile;
    }

    @Nonnull
    @Override
    public List<DiffContent> getContents() {
        return myContents;
    }

    @Nonnull
    @Override
    public List<byte[]> getByteContents() {
        return myByteContents;
    }

    @Nullable
    @Override
    public String getTitle() {
        return myTitle;
    }

    @Nonnull
    @Override
    public List<String> getContentTitles() {
        return myTitles;
    }

    @Override
    @RequiredUIAccess
    public void applyResult(@Nonnull MergeResult result) {
        final byte[] applyContent = switch (result) {
            case CANCEL -> myOriginalContent;
            case LEFT -> ThreeSide.LEFT.select(myByteContents);
            case RIGHT -> ThreeSide.RIGHT.select(myByteContents);
            case RESOLVED -> null;
            default -> throw new IllegalArgumentException(result.toString());
        };

        if (applyContent != null) {
            CommandProcessor.getInstance().newCommand()
                .inWriteAction()
                .run(() -> {
                    try {
                        VirtualFile file = myFile.getFile();
                        if (!DiffImplUtil.makeWritable(myProject, file)) {
                            throw new IOException("File is read-only: " + file.getPresentableName());
                        }
                        file.setBinaryContent(applyContent);
                    }
                    catch (IOException e) {
                        LOG.error(e);
                        Messages.showErrorDialog(myProject, "Can't apply result", CommonLocalize.titleError().get());
                    }
                });
        }

        if (myApplyCallback != null) {
            myApplyCallback.accept(result);
        }
    }
}
