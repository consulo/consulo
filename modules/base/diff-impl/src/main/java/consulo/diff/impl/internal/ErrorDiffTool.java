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
package consulo.diff.impl.internal;

import consulo.application.Application;
import consulo.diff.DiffContext;
import consulo.diff.DiffContextEx;
import consulo.diff.FrameDiffTool;
import consulo.diff.content.DiffContent;
import consulo.diff.content.FileContent;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.request.ComponentDiffRequest;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.MessageDiffRequest;
import consulo.diff.request.UnknownFileTypeDiffRequest;
import consulo.language.file.FileTypeManager;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import org.jspecify.annotations.Nullable;

import java.util.List;

@SuppressWarnings("ExtensionImplIsNotAnnotated")
public class ErrorDiffTool implements FrameDiffTool {
    public static final ErrorDiffTool INSTANCE = new ErrorDiffTool();

    @Override
    @RequiredUIAccess
    public DiffViewer createComponent(DiffContext context, DiffRequest request) {
        return new MyViewer(context, request);
    }

    @Override
    public boolean canShow(DiffContext context, DiffRequest request) {
        return true;
    }

    @Override
    public String getName() {
        return DiffLocalize.errorViewer().get();
    }

    private static class MyViewer implements DiffViewer {
        private final DiffContext myContext;
        private final DiffRequest myRequest;
        private final Component myPanel;

        @RequiredUIAccess
        public MyViewer(DiffContext context, DiffRequest request) {
            myContext = context;
            myRequest = request;

            myPanel = createComponent(request);
        }

        @RequiredUIAccess
        private Component createComponent(DiffRequest request) {
            if (request instanceof MessageDiffRequest messageDiffRequest) {
                // TODO: explain some of ErrorDiffRequest exceptions ?
                String message = messageDiffRequest.getMessage();
                return UnifiedDiffUtil.createMessagePanel(LocalizeValue.of(message));
            }
            if (request instanceof ComponentDiffRequest componentDiffRequest) {
                return componentDiffRequest.getComponent(myContext);
            }
            if (request instanceof ContentDiffRequest contentDiffRequest) {
                List<DiffContent> contents = contentDiffRequest.getContents();
                for (DiffContent content : contents) {
                    if (content instanceof FileContent fileContent && UnknownFileType.INSTANCE == content.getContentType()) {
                        VirtualFile file = fileContent.getFile();

                        UnknownFileTypeDiffRequest unknownFileTypeRequest = new UnknownFileTypeDiffRequest(file, myRequest.getTitle());
                        return unknownFileTypeRequest.getComponent(myContext);
                    }
                }
            }

            return UnifiedDiffUtil.createMessagePanel(DiffLocalize.errorMessageCannotShowDiff());
        }

        @Override
        public Component getComponent() {
            return myPanel;
        }

        @Override
        public @Nullable Component getPreferredFocusedComponent() {
            return null;
        }

        @Override
        @RequiredUIAccess
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

        @Override
        @RequiredUIAccess
        public void dispose() {
        }
    }
}
