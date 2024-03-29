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
package consulo.ide.impl.idea.diff.tools;

import consulo.ide.impl.idea.diff.DiffContext;
import consulo.ide.impl.idea.diff.DiffContextEx;
import consulo.ide.impl.idea.diff.FrameDiffTool;
import consulo.diff.content.DiffContent;
import consulo.diff.content.FileContent;
import consulo.ide.impl.idea.diff.requests.*;
import consulo.ide.impl.idea.diff.util.DiffUtil;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.MessageDiffRequest;
import consulo.language.file.FileTypeManager;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;

import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;
import java.util.List;

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
    return "Error viewer";
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
      if (request instanceof MessageDiffRequest) {
        // TODO: explain some of ErrorDiffRequest exceptions ?
        String message = ((MessageDiffRequest)request).getMessage();
        return DiffUtil.createMessagePanel(message);
      }
      if (request instanceof ComponentDiffRequest) {
        return ((ComponentDiffRequest)request).getComponent(myContext);
      }
      if (request instanceof ContentDiffRequest) {
        List<DiffContent> contents = ((ContentDiffRequest)request).getContents();
        for (final DiffContent content : contents) {
          if (content instanceof FileContent && UnknownFileType.INSTANCE == content.getContentType()) {
            final VirtualFile file = ((FileContent)content).getFile();

            UnknownFileTypeDiffRequest unknownFileTypeRequest = new UnknownFileTypeDiffRequest(file, myRequest.getTitle());
            return unknownFileTypeRequest.getComponent(myContext);
          }
        }
      }

      return DiffUtil.createMessagePanel("Can't show diff");
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
      return myPanel;
    }

    @jakarta.annotation.Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
      return null;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public ToolbarComponents init() {
      if (myRequest instanceof UnknownFileTypeDiffRequest) {
        String fileName = ((UnknownFileTypeDiffRequest)myRequest).getFileName();
        if (fileName != null && FileTypeManager.getInstance().getFileTypeByFileName(fileName) != UnknownFileType.INSTANCE) {
          // FileType was assigned elsewhere (ex: by other UnknownFileTypeDiffRequest). We should reload request.
          if (myContext instanceof DiffContextEx) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
              @Override
              public void run() {
                ((DiffContextEx)myContext).reloadDiffRequest();
              }
            }, IdeaModalityState.current());
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
