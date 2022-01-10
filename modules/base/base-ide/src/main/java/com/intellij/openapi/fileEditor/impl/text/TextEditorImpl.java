/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.fileEditor.impl.text;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.text.TextEditorComponentContainerFactory;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author Vladimir Kondratyev
 */
public class TextEditorImpl extends UserDataHolderBase implements TextEditor {
  public final Project myProject;

  private final PropertyChangeSupport myChangeSupport;
  @Nonnull
  private final TextEditorComponent myComponent;

  @Nonnull
  public final VirtualFile myFile;
 
  private final AsyncEditorLoader myAsyncLoader;

  protected final TextEditorComponentContainerFactory myTextEditorComponentContainerFactory;

  @RequiredUIAccess
  public TextEditorImpl(@Nonnull final Project project, @Nonnull final VirtualFile file, final TextEditorProviderImpl provider) {
    myProject = project;
    myFile = file;
    myChangeSupport = new PropertyChangeSupport(this);
    myTextEditorComponentContainerFactory = provider.myTextEditorComponentContainerFactory;
    myComponent = createEditorComponent(project, file);
    Disposer.register(this, myComponent);

    myAsyncLoader = new AsyncEditorLoader(this, myComponent, provider);
    myAsyncLoader.start();
  }

  @Nonnull
  public Runnable loadEditorInBackground() {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(myFile, scheme, myProject);
    EditorEx editor = (EditorEx)getEditor();
    highlighter.setText(editor.getDocument().getImmutableCharSequence());
    return () -> editor.setHighlighter(highlighter);
  }

  @Nonnull
  protected TextEditorComponent createEditorComponent(final Project project, final VirtualFile file) {
    return new TextEditorComponent(project, file, this, myTextEditorComponentContainerFactory);
  }

  @Override
  public void dispose() {
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return myComponent.getComponentContainer().getComponent();
  }

  @Nullable
  @Override
  public Component getUIComponent() {
    return myComponent.getComponentContainer().getUIComponent();
  }

  @Nullable
  @Override
  public Component getPreferredFocusedUIComponent() {
    return getUIComponent();
  }

  @Override
  @Nonnull
  public JComponent getPreferredFocusedComponent() {
    return getActiveEditor().getContentComponent();
  }

  @Override
  @Nonnull
  public Editor getEditor() {
    return getActiveEditor();
  }

  /**
   * @see DesktopTextEditorComponent#getEditor()
   */
  @Nonnull
  private Editor getActiveEditor() {
    return myComponent.getEditor();
  }

  @Override
  @Nonnull
  public String getName() {
    return "Text";
  }

  @Override
  @Nonnull
  public FileEditorState getState(@Nonnull FileEditorStateLevel level) {
    return myAsyncLoader.getEditorState(level);
  }

  @Override
  public void setState(@Nonnull final FileEditorState state) {
    myAsyncLoader.setEditorState((TextEditorState)state, true);
  }

  @Override
  public boolean isModified() {
    return myComponent.isModified();
  }

  @Override
  public boolean isValid() {
    return myComponent.isEditorValid();
  }

  @Override
  public void selectNotify() {
    myComponent.selectNotify();
  }

  @Override
  public void deselectNotify() {
  }

  public void updateModifiedProperty() {
    myComponent.updateModifiedProperty();
  }

  public void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
    myChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
  }

  @Override
  public void addPropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    myChangeSupport.addPropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    myChangeSupport.removePropertyChangeListener(listener);
  }

  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    return null;
  }

  @Override
  public FileEditorLocation getCurrentLocation() {
    return new TextEditorLocation(getEditor().getCaretModel().getLogicalPosition(), this);
  }

  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    Document document = myComponent.getEditor().getDocument();
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file == null || !file.isValid()) return null;
    return StructureViewBuilder.PROVIDER.getStructureViewBuilder(file.getFileType(), file, myProject);
  }

  @Nullable
  @Override
  public VirtualFile getFile() {
    return FileDocumentManager.getInstance().getFile(myComponent.getEditor().getDocument());
  }

  @Override
  public boolean canNavigateTo(@Nonnull final Navigatable navigatable) {
    return navigatable instanceof OpenFileDescriptor && (((OpenFileDescriptor)navigatable).getLine() != -1 || ((OpenFileDescriptor)navigatable).getOffset() >= 0);
  }

  @Override
  public void navigateTo(@Nonnull final Navigatable navigatable) {
    ((OpenFileDescriptor)navigatable).navigateIn(getEditor());
  }

  @Override
  public String toString() {
    return "Editor: " + myComponent.getFile();
  }
}
