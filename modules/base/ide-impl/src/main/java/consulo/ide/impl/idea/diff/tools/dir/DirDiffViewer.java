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
package consulo.ide.impl.idea.diff.tools.dir;

import consulo.application.HelpManager;
import consulo.dataContext.DataManager;
import consulo.diff.content.DiffContent;
import consulo.diff.content.EmptyContent;
import consulo.diff.content.FileContent;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.diff.DiffContext;
import consulo.ide.impl.idea.diff.FrameDiffTool;
import consulo.ide.impl.idea.diff.contents.DirectoryContent;
import consulo.ide.impl.idea.ide.diff.ArchiveFileDiffElement;
import consulo.ide.impl.idea.ide.diff.DiffElement;
import consulo.ide.impl.idea.ide.diff.DirDiffSettings;
import consulo.ide.impl.idea.ide.diff.VirtualFileDiffElement;
import consulo.ide.impl.idea.openapi.diff.impl.dir.DirDiffFrame;
import consulo.ide.impl.idea.openapi.diff.impl.dir.DirDiffPanel;
import consulo.ide.impl.idea.openapi.diff.impl.dir.DirDiffTableModel;
import consulo.ide.impl.idea.openapi.diff.impl.dir.DirDiffWindow;
import consulo.project.Project;
import consulo.project.internal.DefaultProjectFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

class DirDiffViewer implements FrameDiffTool.DiffViewer {
  @Nonnull
  private final DiffContext myContext;
  @Nonnull
  private final ContentDiffRequest myRequest;

  @Nonnull
  private final DirDiffPanel myDirDiffPanel;
  @Nonnull
  private final JPanel myPanel;

  public DirDiffViewer(@Nonnull DiffContext context, @Nonnull ContentDiffRequest request) {
    myContext = context;
    myRequest = request;

    List<DiffContent> contents = request.getContents();
    DiffElement element1 = createDiffElement(contents.get(0));
    DiffElement element2 = createDiffElement(contents.get(1));

    Project project = context.getProject();
    if (project == null) project = DefaultProjectFactory.getInstance().getDefaultProject();

    DirDiffTableModel model = new DirDiffTableModel(project, element1, element2, new DirDiffSettings());

    myDirDiffPanel = new DirDiffPanel(model, new DirDiffWindow((DirDiffFrame)null) {
      @Override
      public Window getWindow() {
        return null;
      }

      @Override
      public Disposable getDisposable() {
        return DirDiffViewer.this;
      }

      @Override
      public void setTitle(String title) {
      }
    });

    myPanel = new JPanel(new BorderLayout());
    myPanel.add(myDirDiffPanel.getPanel(), BorderLayout.CENTER);
    DataManager.registerDataProvider(myPanel,
      dataId -> HelpManager.HELP_ID == dataId ? "reference.dialogs.diff.folder" : myDirDiffPanel.getData(dataId)
    );
  }

  @Nonnull
  @Override
  public FrameDiffTool.ToolbarComponents init() {
    myDirDiffPanel.setupSplitter();

    FrameDiffTool.ToolbarComponents components = new FrameDiffTool.ToolbarComponents();
    // we return ActionGroup to avoid registering of actions shortcuts
    // * they are already registered inside DirDiffPanel
    // * this fixes conflict between FilterPanel and SynchronizeDiff action for the 'Enter' shortcut
    components.toolbarActions =  Collections.<AnAction>singletonList(new DefaultActionGroup(myDirDiffPanel.getActions()));
    components.statusPanel = myDirDiffPanel.extractFilterPanel();
    return components;
  }

  @Override
  @RequiredUIAccess
  public void dispose() {
    Disposer.dispose(myDirDiffPanel);
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myDirDiffPanel.getTable();
  }

  //
  // Misc
  //

  public static boolean canShowRequest(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    if (!(request instanceof ContentDiffRequest)) return false;
    List<DiffContent> contents = ((ContentDiffRequest)request).getContents();
    if (contents.size() != 2) return false;

    if (!canShowContent(contents.get(0)) || !canShowContent(contents.get(1))) {
      return false;
    }

    return !(contents.get(0) instanceof EmptyContent) || !(contents.get(1) instanceof EmptyContent);
  }

  private static boolean canShowContent(@Nonnull DiffContent content) {
    if (content instanceof EmptyContent || content instanceof DirectoryContent) {
      return true;
    }
    return content instanceof FileContent fileContent && content.getContentType() instanceof ArchiveFileType
      && fileContent.getFile().isValid() && fileContent.getFile().isInLocalFileSystem();
  }

  @Nonnull
  private static DiffElement createDiffElement(@Nonnull DiffContent content) {
    if (content instanceof EmptyContent) {
      return new DiffElement() {
        @Override
        public String getPath() {
          return "";
        }

        @Nonnull
        @Override
        public String getName() {
          return "Nothing";
        }

        @Override
        public long getSize() {
          return -1;
        }

        @Override
        public long getTimeStamp() {
          return -1;
        }

        @Override
        public boolean isContainer() {
          return true;
        }

        @Override
        public DiffElement[] getChildren() throws IOException {
          return EMPTY_ARRAY;
        }

        @Nullable
        @Override
        public byte[] getContent() throws IOException {
          return null;
        }

        @Override
        public Object getValue() {
          return null;
        }
      };
    }
    if (content instanceof DirectoryContent) {
      return new VirtualFileDiffElement(((DirectoryContent)content).getFile());
    }
    if (content instanceof FileContent && content.getContentType() instanceof ArchiveFileType) {
      return new ArchiveFileDiffElement(((FileContent)content).getFile());
    }
    throw new IllegalArgumentException(content.getClass() + " " + content.getContentType());
  }
}
