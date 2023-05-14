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

package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.ide.impl.idea.openapi.module.ModuleConfigurationEditor;
import consulo.configurable.ConfigurationException;
import consulo.ide.setting.module.ModuleConfigurationState;
import consulo.project.Project;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.disposer.CompositeDisposable;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author Eugene Zhuravlev
 * Date: Oct 4, 2003
 * Time: 7:24:37 PM
 */
public abstract class ModuleElementsEditor implements ModuleConfigurationEditor {
  protected final Project myProject;
  protected JComponent myComponent;
  private final CompositeDisposable myDisposables = new CompositeDisposable();

  private final ModuleConfigurationState myState;

  protected ModuleElementsEditor(ModuleConfigurationState state) {
    myProject = state.getProject();
    myState = state;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return getModel() != null && getModel().isChanged();
  }

  protected ModifiableRootModel getModel() {
    return myState.getRootModel();
  }

  protected ModuleConfigurationState getState() {
    return myState;
  }

  public void canApply() throws ConfigurationException {
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
  }

  @RequiredUIAccess
  @Override
  public void reset() {
  }

  @Override
  public void moduleStateChanged() {
  }

  public void moduleCompileOutputChanged(final String baseUrl, final String moduleName) {
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    Disposer.dispose(myDisposables);
  }

  // caching
  @RequiredUIAccess
  @Override
  public final JComponent createComponent(@Nonnull Disposable parentUIDisposable) {
    if (myComponent == null) {
      myComponent = createComponentImpl(parentUIDisposable);
    }
    return myComponent;
  }

  protected void registerDisposable(Disposable disposable) {
    myDisposables.add(disposable);
  }

  @Nonnull
  @RequiredUIAccess
  protected JComponent createComponentImpl(@Nonnull Disposable parentUIDisposable) {
    return (JComponent)TargetAWT.to(createUIComponentImpl(parentUIDisposable));
  }

  @Nullable
  @RequiredUIAccess
  protected Component createUIComponentImpl(@Nonnull Disposable parentUIDisposable) {
    throw new UnsupportedOperationException();
  }
}
