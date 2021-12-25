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
package consulo.ide.welcomeScreen.impl;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 25/12/2021
 */
public class WelcomeVirtualFileEditor extends UserDataHolderBase implements FileEditor {
  private final Project myProject;

  private JComponent myComponent;

  public WelcomeVirtualFileEditor(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    if (myComponent != null) {
      return myComponent;
    }

    myComponent = new JPanel();
    return myComponent;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return null;
  }

  @Nonnull
  @Override
  public String getName() {
    return "welcome";
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void selectNotify() {

  }

  @Override
  public void deselectNotify() {

  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {

  }

  @Override
  public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {

  }

  @Override
  public void dispose() {

  }
}
