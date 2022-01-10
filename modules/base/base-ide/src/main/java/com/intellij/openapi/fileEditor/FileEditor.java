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
package com.intellij.openapi.fileEditor;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.util.dataholder.UserDataHolder;
import kava.beans.PropertyChangeListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 * @see com.intellij.openapi.fileEditor.TextEditor
 */
public interface FileEditor extends UserDataHolder, Disposable {
  /**
   * @see #isModified()
   */
  String PROP_MODIFIED = "modified";
  /**
   * @see #isValid()
   */
  String PROP_VALID = "valid";

  @Nullable
  default Component getUIComponent() {
    return null;
  }

  /**
   * Returns component to be focused when editor is opened.
   */
  @Nullable
  default Component getPreferredFocusedUIComponent() {
    throw new AbstractMethodError();
  }

  /**
   * @return editor's name, a string that identifies editor among
   * other editors. For example, UI form might have two editor: "GUI Designer"
   * and "Text". So "GUI Designer" can be a name of one editor and "Text"
   * can be a name of other editor. The method should never return <code>null</code>.
   */
  @Nonnull
  String getName();

  /**
   * @return editor's internal state. Method should never return <code>null</code>.
   */
  @Nonnull
  default FileEditorState getState(@Nonnull FileEditorStateLevel level) {
    return FileEditorState.INSTANCE;
  }

  /**
   * Applies given state to the editor.
   *
   * @param state cannot be null
   */
  default void setState(@Nonnull FileEditorState state) {
  }

  /**
   * @return whether the editor's content is modified in comparison with its file.
   */
  boolean isModified();

  /**
   * @return whether the editor is valid or not. For some reasons
   * editor can become invalid. For example, text editor becomes invalid when its file is deleted.
   */
  default boolean isValid() {
    return true;
  }

  /**
   * This method is invoked each time when the editor is selected.
   * This can happen in two cases: editor is selected because the selected file
   * has been changed or editor for the selected file has been changed.
   */
  void selectNotify();

  /**
   * This method is invoked each time when the editor is deselected.
   */
  void deselectNotify();

  /**
   * Removes specified listener
   *
   * @param listener to be added
   */
  void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

  /**
   * Adds specified listener
   *
   * @param listener to be removed
   */
  void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

  /**
   * @return highlighter object to perform background analysis and highlighting activities.
   * Return <code>null</code> if no background highlighting activity necessary for this file editor.
   */
  @Nullable
  default BackgroundEditorHighlighter getBackgroundHighlighter() {
    return null;
  }

  /**
   * The method is optional. Currently is used only by find usages subsystem
   *
   * @return the location of user focus. Typically it's a caret or any other form of selection start.
   */
  @Nullable
  default FileEditorLocation getCurrentLocation() {
    return null;
  }

  @Nullable
  default StructureViewBuilder getStructureViewBuilder() {
    return null;
  }

  @Nullable
  default VirtualFile getFile() {
    return null;
  }

  // TODO [VISTALL] AWT & Swing dependency

  // region AWT & Swing dependency

  /**
   * @return component which represents editor in the UI.
   * The method should never return <code>null</code>.
   */
  @Nonnull
  default javax.swing.JComponent getComponent() {
    Component uiComponent = getUIComponent();
    if (uiComponent != null) {
      return (javax.swing.JComponent)TargetAWT.to(uiComponent);
    }
    throw new AbstractMethodError();
  }

  /**
   * Returns component to be focused when editor is opened.
   */
  @Nullable
  default javax.swing.JComponent getPreferredFocusedComponent() {
    throw new AbstractMethodError();
  }

  // endregion
}
