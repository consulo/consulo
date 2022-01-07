/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.EventDispatcher;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.ui.configuration.ContentFolderPropertiesDialog;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventListener;

/**
 * @author Eugene Zhuravlev
 * @since Oct 8, 2003
 */
public abstract class ContentEntryEditor implements ContentRootPanel.ActionCallback {
  private boolean myIsSelected;
  private ContentRootPanel myContentRootPanel;
  private JPanel myMainPanel;
  protected EventDispatcher<ContentEntryEditorListener> myEventDispatcher;
  private final ContentEntry myContentEntry;

  public interface ContentEntryEditorListener extends EventListener {
    default void editingStarted(@Nonnull ContentEntryEditor editor) {}

    default void beforeEntryDeleted(@Nonnull ContentEntryEditor editor) {}

    default void folderAdded(@Nonnull ContentEntryEditor editor, ContentFolder contentFolder) {}

    default void folderRemoved(@Nonnull ContentEntryEditor editor, ContentFolder contentFolder) {}

    default void navigationRequested(@Nonnull ContentEntryEditor editor, VirtualFile file) {}
  }

  public ContentEntryEditor(final ContentEntry contentEntry) {
    myContentEntry = contentEntry;
  }

  public void initUI() {
    myMainPanel = new JPanel(new BorderLayout());
    myMainPanel.setOpaque(false);
    myMainPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        myEventDispatcher.getMulticaster().editingStarted(ContentEntryEditor.this);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        if (!myIsSelected) {
          highlight(true);
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (!myIsSelected) {
          highlight(false);
        }
      }
    });
    myEventDispatcher = EventDispatcher.create(ContentEntryEditorListener.class);
    setSelected(false);
    update();
  }

  @Nonnull
  protected ContentEntry getContentEntry() {
    return myContentEntry;
  }

  protected abstract ModifiableRootModel getModel();

  @Override
  public void deleteContentEntry() {
    final String path = FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(myContentEntry.getUrl()));
    final int answer = Messages.showYesNoDialog(ProjectBundle.message("module.paths.remove.content.prompt", path),
                                                ProjectBundle.message("module.paths.remove.content.title"), Messages.getQuestionIcon());
    if (answer != 0) { // no
      return;
    }
    myEventDispatcher.getMulticaster().beforeEntryDeleted(this);
    final ContentEntry entry = getContentEntry();
    if (entry != null) {
      getModel().removeContentEntry(entry);
    }
  }

  @Override
  public void deleteContentFolder(ContentEntry contentEntry, ContentFolder folder) {
    removeFolder(folder);
    update();
  }

  @Override
  public void showChangeOptionsDialog(ContentEntry contentEntry, ContentFolder contentFolder) {
    ContentFolderPropertiesDialog c = new ContentFolderPropertiesDialog(getModel().getProject(), contentFolder);
    AsyncResult<Boolean> booleanAsyncResult = c.showAndGetOk();
    if(booleanAsyncResult.getResult() == Boolean.TRUE) {
      update();
    }
  }

  @Override
  public void navigateFolder(ContentEntry contentEntry, ContentFolder contentFolder) {
    final VirtualFile file = contentFolder.getFile();
    if (file != null) { // file can be deleted externally
      myEventDispatcher.getMulticaster().navigationRequested(this, file);
    }
  }

  public void addContentEntryEditorListener(ContentEntryEditorListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeContentEntryEditorListener(ContentEntryEditorListener listener) {
    myEventDispatcher.removeListener(listener);
  }

  public void setSelected(boolean isSelected) {
    if (myIsSelected != isSelected) {
      highlight(isSelected);
      myIsSelected = isSelected;
    }
  }

  private void highlight(boolean selected) {
    if (myContentRootPanel != null) {
      myContentRootPanel.setSelected(selected);
    }
  }

  public JComponent getComponent() {
    return myMainPanel;
  }

  public void update() {
    if (myContentRootPanel != null) {
      myMainPanel.remove(myContentRootPanel);
    }
    myContentRootPanel = createContentRootPane();
    myContentRootPanel.initUI();
    myContentRootPanel.setSelected(myIsSelected);
    myMainPanel.add(myContentRootPanel, BorderLayout.CENTER);
    myMainPanel.revalidate();
  }

  protected ContentRootPanel createContentRootPane() {
    return new ContentRootPanel(this) {
      @Nonnull
      @Override
      protected ContentEntry getContentEntry() {
        return ContentEntryEditor.this.getContentEntry();
      }
    };
  }

  @Nullable
  public ContentFolder addFolder(@Nonnull final VirtualFile file, ContentFolderTypeProvider contentFolderType) {
    final ContentEntry contentEntry = getContentEntry();
    if (contentEntry != null) {
      final ContentFolder contentFolder = contentEntry.addFolder(file, contentFolderType);
      try {
        return contentFolder;
      }
      finally {
        myEventDispatcher.getMulticaster().folderAdded(this, contentFolder);
        update();
      }
    }

    return null;
  }


  public void removeFolder(@Nonnull final ContentFolder contentFolder) {
    try {
      if (contentFolder.isSynthetic()) {
        return;
      }
      final ContentEntry contentEntry = getContentEntry();
      if (contentEntry != null) {
        contentEntry.removeFolder(contentFolder);
      }
    }
    finally {
      myEventDispatcher.getMulticaster().folderRemoved(this, contentFolder);
      update();
    }
  }

  @Nullable
  public ContentFolder getFolder(@Nonnull final VirtualFile file) {
    final ContentEntry contentEntry = getContentEntry();
    if (contentEntry == null) {
      return null;
    }
    for (ContentFolder contentFolder : contentEntry.getFolders(ContentFolderScopes.all())) {
      final VirtualFile f = contentFolder.getFile();
      if (f != null && f.equals(file)) {
        return contentFolder;
      }
    }
    return null;
  }
}
