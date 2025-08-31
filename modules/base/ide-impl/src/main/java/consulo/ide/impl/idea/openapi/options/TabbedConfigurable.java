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
package consulo.ide.impl.idea.openapi.options;

import consulo.configurable.CompositeConfigurable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.TabbedPaneWrapper;
import consulo.configurable.Configurable;
import consulo.disposer.Disposable;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public abstract class TabbedConfigurable extends CompositeConfigurable<Configurable> {
  protected TabbedPaneWrapper myTabbedPane;
  private final Disposable myParent;

  protected TabbedConfigurable(@Nonnull Disposable parent) {
    myParent = parent;
  }

  @Override
  public JComponent createComponent() {
    myTabbedPane = new TabbedPaneWrapper(myParent);
    createConfigurableTabs();
    JComponent component = myTabbedPane.getComponent();
    component.setPreferredSize(new Dimension(500, 400));
    return component;
  }

  protected void createConfigurableTabs() {
    for (Configurable configurable : getConfigurables()) {
      myTabbedPane.addTab(configurable.getDisplayName().get(), configurable.createComponent());
    }
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myTabbedPane = null;
    super.disposeUIResources();
  }
}
