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
package consulo.ide.impl.idea.ide.actions;

import consulo.dataContext.DataManager;
import consulo.ui.ex.TreeExpander;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.function.Function;

/**
 * @author max
 */
public abstract class TreeCollapseAllActionBase extends DumbAwareAction implements DumbAware {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    TreeExpander expander = getExpanderMaybeFromToolWindow(e, this::getExpander);
    if (expander == null) {
      return;
    }
    if (!expander.canCollapse()) {
      return;
    }
    expander.collapseAll();
  }

  @Nullable
  protected abstract TreeExpander getExpander(DataContext dataContext);

  @RequiredUIAccess
  @Override
  public void update(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    TreeExpander expander = getExpanderMaybeFromToolWindow(event, this::getExpander);
    presentation.setEnabled(expander != null && expander.canCollapse() && expander.isCollapseAllVisible());
  }

  @Nullable
  public static TreeExpander getExpanderMaybeFromToolWindow(AnActionEvent e, Function<DataContext, TreeExpander> getExpander) {
    TreeExpander expander = getExpander.apply(e.getDataContext());
    if (expander != null) {
      return expander;
    }

    ToolWindow toolWindow = e.getData(ToolWindow.KEY);
    if (toolWindow == null) {
      return null;
    }

    ContentManager contentManager = toolWindow.getContentManagerIfCreated();
    if (contentManager == null) {
      return null;
    }

    Content selectedContent = contentManager.getSelectedContent();
    if (selectedContent == null) {
      return null;
    }

    JComponent component = selectedContent.getComponent();
    if (component == null) {
      return null;
    }

    DataContext context = DataManager.getInstance().getDataContext(component);
    return getExpander.apply(context);
  }
}
