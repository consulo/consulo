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

import consulo.fileEditor.highlight.BackgroundEditorHighlighter;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.document.impl.DocumentImpl;
import consulo.fileEditor.text.TextEditorState;
import consulo.fileEditor.FileEditorLocation;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.FileEditorStateLevel;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import consulo.navigation.Navigatable;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import kava.beans.PropertyChangeListener;

/**
 * @author nik
 */
public class HttpFileEditor implements TextEditor {
  private final UserDataHolderBase myUserDataHolder = new UserDataHolderBase();
  private final RemoteFilePanel myPanel;
  private Editor myMockTextEditor;
  private final Project myProject;

  public HttpFileEditor(Project project, HttpVirtualFile virtualFile) {
    myProject = project;
    myPanel = new RemoteFilePanel(project, virtualFile);
  }

  
  public JComponent getComponent() {
    return myPanel.getMainPanel();
  }

  public JComponent getPreferredFocusedComponent() {
    TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getPreferredFocusedComponent();
    }
    return myPanel.getMainPanel();
  }

  
  public String getName() {
    return "Http";
  }

  
  public Editor getEditor() {
    TextEditor fileEditor = myPanel.getFileEditor();
    if (fileEditor != null) {
      return fileEditor.getEditor();
    }
    if (myMockTextEditor == null) {
      myMockTextEditor = EditorFactory.getInstance().createViewer(new DocumentImpl(""), myProject);
    }
    return myMockTextEditor;
  }

  
  public FileEditorState getState(FileEditorStateLevel level) {
    TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getState(level);
    }
    return new TextEditorState();
  }

  public void setState(FileEditorState state) {
    TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      textEditor.setState(state);
    }
  }

  public boolean isModified() {
    return false;
  }

  public boolean isValid() {
    return true;
  }

  public void selectNotify() {
    myPanel.selectNotify();
  }

  public void deselectNotify() {
    myPanel.deselectNotify();
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    myPanel.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    myPanel.removePropertyChangeListener(listener);
  }

  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getBackgroundHighlighter();
    }
    return null;
  }

  public boolean canNavigateTo(Navigatable navigatable) {
    TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.canNavigateTo(navigatable);
    }
    return false;
  }

  public void navigateTo(Navigatable navigatable) {
    TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      textEditor.navigateTo(navigatable);
    }
  }

  public <T> T getUserData(Key<T> key) {
    TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getUserData(key);
    }
    return myUserDataHolder.getUserData(key);
  }

  public <T> void putUserData(Key<T> key, @Nullable T value) {
    TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      textEditor.putUserData(key, value);
    }
    else {
      myUserDataHolder.putUserData(key, value);
    }
  }

  public FileEditorLocation getCurrentLocation() {
    TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getCurrentLocation();
    }
    return null;
  }

  public StructureViewBuilder getStructureViewBuilder() {
    TextEditor textEditor = myPanel.getFileEditor();
    if (textEditor != null) {
      return textEditor.getStructureViewBuilder();
    }
    return null;
  }

  @Override
  public @Nullable VirtualFile getFile() {
    return null;
  }

  public void dispose() {
    if (myMockTextEditor != null) {
      EditorFactory.getInstance().releaseEditor(myMockTextEditor);
    }
    myPanel.dispose();
  }
}
