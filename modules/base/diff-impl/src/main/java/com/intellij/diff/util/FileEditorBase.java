/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.diff.util;

import com.intellij.openapi.fileEditor.FileEditor;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;

import javax.annotation.Nonnull;

// from kotlin
public abstract class FileEditorBase extends UserDataHolderBase implements FileEditor {
  private PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

  public FileEditorBase() {
    //putUserData(FileEditorManagerImpl.SINGLETON_EDITOR_IN_WINDOW, true);
    //putUserData(DockManagerImpl.SHOW_NORTH_PANEL, false);
  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    myPropertyChangeSupport.addPropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    myPropertyChangeSupport.removePropertyChangeListener(listener);
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void dispose() {

  }

  @Override
  public void selectNotify() {

  }

  @Override
  public void deselectNotify() {
    
  }
}
