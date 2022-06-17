/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.awt.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.ex.awt.TabbedPane;
import consulo.ui.ex.awt.TabbedPaneWrapper;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 16-Apr-22
 */
@Service(ComponentScope.APPLICATION)
public interface TabFactoryBuilder {
  public interface TabFactory {
    TabbedPane createTabbedPane(int tabPlacement);

    TabbedPaneHolder createTabbedPaneHolder();

    TabbedPaneWrapper.TabWrapper createTabWrapper(JComponent component);
  }

  static TabFactoryBuilder getInstance() {
    return Application.get().getInstance(TabFactoryBuilder.class);
  }

  TabFactory createJTabbedPanel(TabbedPaneWrapper wrapper);

  TabFactory createEditorTabPanel(TabbedPaneWrapper wrapper, Project project, @Nonnull Disposable parent);
}
