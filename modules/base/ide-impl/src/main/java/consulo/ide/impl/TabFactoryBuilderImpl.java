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
package consulo.ide.impl;

import consulo.ide.impl.idea.ui.JBTabsPaneImpl;
import consulo.ide.impl.idea.ui.TabbedPaneImpl;
import consulo.ide.impl.idea.ui.tabs.JBTabs;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.ex.PrevNextActionsDescriptor;
import consulo.ui.ex.awt.TabbedPane;
import consulo.ui.ex.awt.TabbedPaneWrapper;
import consulo.ui.ex.awt.internal.TabFactoryBuilder;
import consulo.ui.ex.awt.internal.TabbedPaneHolder;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 16-Apr-22
 */
@Singleton
public class TabFactoryBuilderImpl implements TabFactoryBuilder {

  private static class JTabbedPaneFactory implements TabFactory {
    private final TabbedPaneWrapper myWrapper;

    private JTabbedPaneFactory(TabbedPaneWrapper wrapper) {
      myWrapper = wrapper;
    }

    @Override
    public TabbedPane createTabbedPane(int tabPlacement) {
      return new TabbedPaneImpl(tabPlacement);
    }

    @Override
    public TabbedPaneHolder createTabbedPaneHolder() {
      return new TabbedPaneHolder(myWrapper);
    }

    @Override
    public TabbedPaneWrapper.TabWrapper createTabWrapper(JComponent component) {
      return new TabbedPaneWrapper.TabWrapper(component);
    }
  }

  private static class JBTabsFactory implements TabFactory {

    private final Project myProject;
    private final Disposable myParent;
    private final TabbedPaneWrapper myWrapper;

    private JBTabsFactory(TabbedPaneWrapper wrapper, Project project, @Nonnull Disposable parent) {
      myWrapper = wrapper;
      myProject = project;
      myParent = parent;
    }

    @Override
    public TabbedPane createTabbedPane(int tabPlacement) {
      return new JBTabsPaneImpl(myProject, tabPlacement, myParent);
    }

    @Override
    public TabbedPaneHolder createTabbedPaneHolder() {
      return new TabbedPaneHolder(myWrapper) {
        @Override
        public boolean requestDefaultFocus() {
          getTabs().requestFocus();
          return true;
        }

      };
    }

    @Override
    public TabbedPaneWrapper.TabWrapper createTabWrapper(JComponent component) {
      final TabbedPaneWrapper.TabWrapper tabWrapper = new TabbedPaneWrapper.TabWrapper(component);
      tabWrapper.setCustomFocus(false);
      return tabWrapper;
    }

    public JBTabs getTabs() {
      return ((JBTabsPaneImpl)myWrapper.getTabbedPane()).getTabs();
    }

    public void dispose() {
    }
  }

  public static class AsJBTabs extends TabbedPaneWrapper {
    public AsJBTabs(@Nullable Project project, int tabPlacement, PrevNextActionsDescriptor installKeyboardNavigation, @Nonnull Disposable parent) {
      super(false);
      init(tabPlacement, installKeyboardNavigation, new JBTabsFactory(this, project, parent));
    }

    public JBTabs getTabs() {
      return ((JBTabsPaneImpl)myTabbedPane).getTabs();
    }
  }

  public static class AsJTabbedPane extends TabbedPaneWrapper {
    public AsJTabbedPane(int tabPlacement) {
      super(false);
      init(tabPlacement, TabbedPaneWrapper.DEFAULT_PREV_NEXT_SHORTCUTS, new JTabbedPaneFactory(this));
    }
  }

  @Override
  public TabFactory createJTabbedPanel(TabbedPaneWrapper wrapper) {
    return new JTabbedPaneFactory(wrapper);
  }

  @Override
  public TabFactory createEditorTabPanel(TabbedPaneWrapper wrapper, Project project, @Nonnull Disposable parent) {
    return new JBTabsFactory(wrapper, project, parent);
  }
}
