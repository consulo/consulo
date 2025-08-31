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
package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.ide.impl.idea.openapi.roots.ui.configuration.classpath.ClasspathPanelImpl;
import consulo.disposer.Disposable;
import consulo.ide.setting.module.ModuleConfigurationState;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.ProjectBundle;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import javax.swing.*;

/**
 * @author Eugene Zhuravlev
 * @since 2003-10-04
 */
public class ClasspathEditor extends ModuleElementsEditor implements ModuleRootListener {
  private ClasspathPanelImpl myPanel;

  public ClasspathEditor(ModuleConfigurationState state) {
    super(state);

    Disposable disposable = Disposable.newDisposable();

    state.getProject().getMessageBus().connect(disposable).subscribe(ModuleRootListener.class, this);
    registerDisposable(disposable);
  }

  @Override
  public LocalizeValue getDisplayName() {
    return ProjectLocalize.modulesClasspathTitle();
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

  public void selectOrderEntry(@Nonnull OrderEntry entry) {
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
