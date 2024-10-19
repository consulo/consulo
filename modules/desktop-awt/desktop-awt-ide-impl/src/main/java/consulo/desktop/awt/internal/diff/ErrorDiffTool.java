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
package consulo.desktop.awt.internal.diff;

import consulo.application.Application;
import consulo.desktop.awt.internal.diff.util.AWTDiffUtil;
import consulo.diff.DiffContext;
import consulo.diff.DiffContextEx;
import consulo.diff.FrameDiffTool;
import consulo.diff.content.DiffContent;
import consulo.diff.content.FileContent;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.MessageDiffRequest;
import consulo.ide.impl.idea.diff.requests.ComponentDiffRequest;
import consulo.ide.impl.idea.diff.requests.UnknownFileTypeDiffRequest;
import consulo.language.file.FileTypeManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;

@SuppressWarnings("ExtensionImplIsNotAnnotated")
public class ErrorDiffTool implements FrameDiffTool {
    public static final ErrorDiffTool INSTANCE = new ErrorDiffTool();

    @RequiredUIAccess
    @Nonnull
    @Override
    public DiffViewer createComponent(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
        return new MyViewer(context, request);
    }

    @Override
    public boolean canShow(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
        return true;
    }

    @Nonnull
    @Override
    public String getName() {
        return DiffLocalize.errorViewer().get();
    }

    private static class MyViewer implements DiffViewer {
        @Nonnull
        private final DiffContext myContext;
        @Nonnull
        private final DiffRequest myRequest;

        @Nonnull
        private final JPanel myPanel;

        public MyViewer(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
            myContext = context;
            myRequest = request;

            myPanel = JBUI.Panels.simplePanel(createComponent(request));
        }

        @Nonnull
        private JComponent createComponent(@Nonnull DiffRequest request) {
            if (request instanceof MessageDiffRequest messageDiffRequest) {
                // TODO: explain some of ErrorDiffRequest exceptions ?
                String message = messageDiffRequest.getMessage();
                return AWTDiffUtil.createMessagePanel(message);
            }
            if (request instanceof ComponentDiffRequest componentDiffRequest) {
                return componentDiffRequest.getComponent(myContext);
            }
            if (request instanceof ContentDiffRequest contentDiffRequest) {
                List<DiffContent> contents = contentDiffRequest.getContents();
                for (final DiffContent content : contents) {
                    if (content instanceof FileContent fileContent && UnknownFileType.INSTANCE == content.getContentType()) {
                        final VirtualFile file = fileContent.getFile();

                        UnknownFileTypeDiffRequest unknownFileTypeRequest = new UnknownFileTypeDiffRequest(file, myRequest.getTitle());
                        return unknownFileTypeRequest.getComponent(myContext);
                    }
                }
            }

            return AWTDiffUtil.createMessagePanel(DiffLocalize.errorMessageCannotShowDiff().get());
        }

        @Nonnull
        @Override
        public JComponent getComponent() {
            return myPanel;
        }

        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
            return null;
        }

        @RequiredUIAccess
        @Nonnull
        @Override
        public ToolbarComponents init() {
            if (myRequest instanceof UnknownFileTypeDiffRequest unknownFileTypeDiffRequest) {
                String fileName = unknownFileTypeDiffRequest.getFileName();
                if (fileName != null && FileTypeManager.getInstance().getFileTypeByFileName(fileName) != UnknownFileType.INSTANCE) {
                    // FileType was assigned elsewhere (ex: by other UnknownFileTypeDiffRequest). We should reload request.
                    if (myContext instanceof DiffContextEx diffContextEx) {
                        Application application = Application.get();
                        application.invokeLater(diffContextEx::reloadDiffRequest, application.getCurrentModalityState());
                    }
                }
            }

            return new ToolbarComponents();
        }

        @RequiredUIAccess
        @Override
        public void dispose() {
        }
    }
}
