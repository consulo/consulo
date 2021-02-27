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
package com.intellij.ide.actions;

import com.intellij.ide.DataManager;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    if(expander != null) {
      return expander;
    }

    ToolWindow toolWindow = e.getData(PlatformDataKeys.TOOL_WINDOW);
    if(toolWindow == null) {
      return null;
    }

    ContentManager contentManager = toolWindow.getContentManagerIfCreated();
    if(contentManager == null) {
      return null;
    }

    Content selectedContent = contentManager.getSelectedContent();
    if(selectedContent == null) {
      return null;
    }

    JComponent component = selectedContent.getComponent();
    if(component == null) {
      return null;
    }

    DataContext context = DataManager.getInstance().getDataContext(component);
    return getExpander.apply(context);
  }
}
