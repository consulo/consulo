/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.fileEditor;

import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorLocation;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.FileEditorStateLevel;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.ui.Component;
import consulo.ui.Label;
import kava.beans.PropertyChangeListener;
import org.jspecify.annotations.Nullable;


/**
 * @author VISTALL
 * @since 30.05.14
 */
public class SandFileEditor extends UserDataHolderBase implements FileEditor {
  
  @Override
  public Component getUIComponent() {
    return Label.create("Hello World");
  }

  
  @Override
  public String getName() {
    return "Sand";
  }

  
  @Override
  public FileEditorState getState(FileEditorStateLevel level) {
    return FileEditorState.INSTANCE;
  }

  @Override
  public void setState(FileEditorState state) {

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

  }

  @Override
  public void deselectNotify() {

  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {

  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {

  }

  @Override
  public @Nullable FileEditorLocation getCurrentLocation() {
    return null;
  }

  @Override
  public void dispose() {

  }
}
