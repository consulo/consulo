/*
 * Copyright 2004-2005 Alexey Efimov
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

import com.intellij.ProjectTopics;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanelImpl;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author Eugene Zhuravlev
 *         Date: Oct 4, 2003
 *         Time: 6:54:57 PM
 */
public class ClasspathEditor extends ModuleElementsEditor implements ModuleRootListener {
  private ClasspathPanelImpl myPanel;

  public ClasspathEditor(final ModuleConfigurationState state) {
    super(state);

    final Disposable disposable = Disposable.newDisposable();

    state.getProject().getMessageBus().connect(disposable).subscribe(ProjectTopics.PROJECT_ROOTS, this);
    registerDisposable(disposable);
  }

  @Override
  public String getDisplayName() {
    return ProjectBundle.message("modules.classpath.title");
  }

  @Override
  public void saveData() {
    myPanel.stopEditing();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public JComponent createComponentImpl(Disposable parentUIDisposable) {
    return myPanel = new ClasspathPanelImpl(getState());
  }

  public void selectOrderEntry(@Nonnull final OrderEntry entry) {
    myPanel.selectOrderEntry(entry);
  }

  @Override
  public void moduleStateChanged() {
    if (myPanel != null) {
      myPanel.initFromModel();
    }
  }

  @Override
  public void rootsChanged(ModuleRootEvent event) {
    if (myPanel != null) {
      myPanel.rootsChanged();
    }
  }
}
