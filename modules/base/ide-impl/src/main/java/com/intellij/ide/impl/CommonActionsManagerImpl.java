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
package com.intellij.ide.impl;

import consulo.ui.ex.action.AutoScrollToSourceOptionProvider;
import com.intellij.ide.DefaultTreeExpander;
import consulo.ui.ex.action.ExporterToTextFile;
import com.intellij.ide.actions.*;
import consulo.application.AllIcons;
import consulo.component.ComponentManager;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.action.ContextHelpAction;
import consulo.ui.ex.awt.AutoScrollToSourceHandler;
import jakarta.inject.Singleton;

import javax.swing.*;

/**
 * @author max
 */
@Singleton
public class CommonActionsManagerImpl extends CommonActionsManager {
  @Override
  public AnAction createPrevOccurenceAction(OccurenceNavigator navigator) {
    return new PreviousOccurenceToolbarAction(navigator);
  }

  @Override
  public AnAction createNextOccurenceAction(OccurenceNavigator navigator) {
    return new NextOccurenceToolbarAction(navigator);
  }

  @Override
  public AnAction createExpandAllAction(TreeExpander expander) {
    return new ExpandAllToolbarAction(expander);
  }

  @Override
  public AnAction createExpandAllAction(TreeExpander expander, JComponent component) {
    final ExpandAllToolbarAction expandAllToolbarAction = new ExpandAllToolbarAction(expander);
    expandAllToolbarAction.registerCustomShortcutSet(expandAllToolbarAction.getShortcutSet(), component);
    return expandAllToolbarAction;
  }

  @Override
  public AnAction createExpandAllHeaderAction(JTree tree) {
    AnAction action = createExpandAllAction(new DefaultTreeExpander(tree), tree);
    action.getTemplatePresentation().setIcon(AllIcons.General.ExpandAll);
    return action;
  }

  @Override
  public AnAction createCollapseAllAction(TreeExpander expander) {
    return new CollapseAllToolbarAction(expander);
  }

  @Override
  public AnAction createCollapseAllAction(TreeExpander expander, JComponent component) {
    final CollapseAllToolbarAction collapseAllToolbarAction = new CollapseAllToolbarAction(expander);
    collapseAllToolbarAction.registerCustomShortcutSet(collapseAllToolbarAction.getShortcutSet(), component);
    return collapseAllToolbarAction;
  }

  @Override
  public AnAction createCollapseAllHeaderAction(JTree tree) {
    AnAction action = createCollapseAllAction(new DefaultTreeExpander(tree), tree);
    action.getTemplatePresentation().setIcon(AllIcons.General.CollapseAll);
    return action;
  }

  @Override
  public AnAction createHelpAction(String helpId) {
    return new ContextHelpAction(helpId);
  }

  public AnAction installAutoscrollToSourceHandler(ComponentManager project, JTree tree, final AutoScrollToSourceOptionProvider optionProvider) {
    AutoScrollToSourceHandler handler = new AutoScrollToSourceHandler() {
      @Override
      public boolean isAutoScrollMode() {
        return optionProvider.isAutoScrollMode();
      }

      @Override
      public void setAutoScrollMode(boolean state) {
        optionProvider.setAutoScrollMode(state);
      }
    };
    handler.install(tree);
    return handler.createToggleAction();
  }

  public AnAction createExportToTextFileAction(ExporterToTextFile exporter) {
    return new ExportToTextFileToolbarAction(exporter);
  }
}
