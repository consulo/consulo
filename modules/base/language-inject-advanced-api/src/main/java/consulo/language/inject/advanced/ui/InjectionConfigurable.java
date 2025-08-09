/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.inject.advanced.ui;

import consulo.configurable.ConfigurationException;
import consulo.language.inject.advanced.Injection;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import javax.swing.*;

public abstract class InjectionConfigurable<T extends Injection, P extends InjectionPanel<T>> extends NamedConfigurable<T> {
  private final Runnable myTreeUpdater;
  protected final T myInjection;
  protected final Project myProject;
  private P myPanel;

  public InjectionConfigurable(T injection, @Nonnull Runnable treeUpdater, Project project) {
    myProject = project;
    myInjection = injection;
    myTreeUpdater = treeUpdater;
  }

  @Override
  public void setDisplayName(String name) {
  }

  @Override
  public T getEditableObject() {
    return myInjection;
  }

  @Override
  public JComponent createOptionsPanel() {
    myPanel = createOptionsPanelImpl();
    myPanel.addUpdater(myTreeUpdater);
    return myPanel.getComponent();
  }

  protected abstract P createOptionsPanelImpl();

  public P getPanel() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    return myPanel.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myPanel.apply();
  }

  @Override
  public void reset() {
    myPanel.reset();
  }

  @Override
  public void disposeUIResources() {
    myPanel = null;
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    P p = getPanel();
    return p != null ? p.getInjection().getDisplayName() : myInjection.getDisplayName();
  }
}
