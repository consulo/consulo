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
package com.intellij.diff.tools.dir;

import com.intellij.diff.DiffContext;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DirectoryContent;
import com.intellij.diff.contents.EmptyContent;
import com.intellij.diff.contents.FileContent;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.ide.DataManager;
import com.intellij.ide.diff.ArchiveFileDiffElement;
import com.intellij.ide.diff.DiffElement;
import com.intellij.ide.diff.DirDiffSettings;
import com.intellij.ide.diff.VirtualFileDiffElement;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diff.impl.dir.DirDiffFrame;
import com.intellij.openapi.diff.impl.dir.DirDiffPanel;
import com.intellij.openapi.diff.impl.dir.DirDiffTableModel;
import com.intellij.openapi.diff.impl.dir.DirDiffWindow;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import consulo.fileTypes.ArchiveFileType;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    DataManager.registerDataProvider(myPanel, new DataProvider() {
      @Override
      public Object getData(@Nonnull @NonNls Key dataId) {
        if (PlatformDataKeys.HELP_ID == dataId) {
          return "reference.dialogs.diff.folder";
        }
        return myDirDiffPanel.getData(dataId);
      }
    });
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

    if (!canShowContent(contents.get(0))) return false;
    if (!canShowContent(contents.get(1))) return false;

    if (contents.get(0) instanceof EmptyContent && contents.get(1) instanceof EmptyContent) return false;

    return true;
  }

  private static boolean canShowContent(@Nonnull DiffContent content) {
    if (content instanceof EmptyContent) return true;
    if (content instanceof DirectoryContent) return true;
    if (content instanceof FileContent &&
        content.getContentType() instanceof ArchiveFileType &&
        ((FileContent)content).getFile().isValid() &&
        ((FileContent)content).getFile().isInLocalFileSystem()) {
      return true;
    }

    return false;
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
