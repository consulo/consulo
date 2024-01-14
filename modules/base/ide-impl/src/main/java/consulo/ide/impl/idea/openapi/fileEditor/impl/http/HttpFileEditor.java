/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.impl.http;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.document.impl.DocumentImpl;
import consulo.fileEditor.*;
import consulo.fileEditor.highlight.BackgroundEditorHighlighter;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.text.TextEditorState;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeListener;

import javax.swing.*;

/**
 * @author nik
 */
public class HttpFileEditor implements TextEditor {
  private final UserDataHolderBase myUserDataHolder = new UserDataHolderBase();
  private final RemoteFilePanel myPanel;
  private Editor myMockTextEditor;
  private final Project myProject;
  private final HttpFileEditorProvider myFileEditorProvider;

  public HttpFileEditor(final Project project, final HttpVirtualFile virtualFile, HttpFileEditorProvider fileEditorProvider) {
    myProject = project;
    myFileEditorProvider = fileEditorProvider;
    myPanel = new RemoteFilePanel(project, virtualFile);
  }

  @Nonnull
  @Override
  public FileEditorProvider getProvider() {
    return myFileEditorProvider;
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return myPanel.getMainPanel();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    final TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getPreferredFocusedComponent();
    }
    return myPanel.getMainPanel();
  }

  @Override
  @Nonnull
  public String getName() {
    return "Http";
  }

  @Override
  @Nonnull
  public Editor getEditor() {
    final TextEditor fileEditor = myPanel.getFileEditor();
    if (fileEditor != null) {
      return fileEditor.getEditor();
    }
    if (myMockTextEditor == null) {
      myMockTextEditor = EditorFactory.getInstance().createViewer(new DocumentImpl(""), myProject);
    }
    return myMockTextEditor;
  }

  @Override
  @Nonnull
  public FileEditorState getState(@Nonnull final FileEditorStateLevel level) {
    final TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getState(level);
    }
    return new TextEditorState();
  }

  @Override
  public void setState(@Nonnull final FileEditorState state) {
    final TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      textEditor.setState(state);
    }
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void selectNotify() {
    myPanel.selectNotify();
  }

  @Override
  public void deselectNotify() {
    myPanel.deselectNotify();
  }

  @Override
  public void addPropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    myPanel.addPropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    myPanel.removePropertyChangeListener(listener);
  }

  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    final TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getBackgroundHighlighter();
    }
    return null;
  }

  @Override
  public boolean canNavigateTo(@Nonnull Navigatable navigatable) {
    final TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.canNavigateTo(navigatable);
    }
    return false;
  }

  @Override
  public void navigateTo(@Nonnull Navigatable navigatable) {
    final TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      textEditor.navigateTo(navigatable);
    }
  }

  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    final TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getUserData(key);
    }
    return myUserDataHolder.getUserData(key);
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    final TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      textEditor.putUserData(key, value);
    }
    else {
      myUserDataHolder.putUserData(key, value);
    }
  }

  @Override
  public FileEditorLocation getCurrentLocation() {
    final TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getCurrentLocation();
    }
    return null;
  }

  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    final TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getStructureViewBuilder();
    }
    return null;
  }

  @Nullable
  @Override
  public VirtualFile getFile() {
    return null;
  }

  @Override
  public void dispose() {
    if (myMockTextEditor != null) {
      EditorFactory.getInstance().releaseEditor(myMockTextEditor);
    }
    myPanel.dispose();
  }
}
