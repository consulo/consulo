/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.xdebugger.impl.evaluate.quick.common;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.concurrency.ResultConsumer;
import com.intellij.icons.AllIcons;
import consulo.disposer.Disposable;
import com.intellij.openapi.actionSystem.*;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.disposer.Disposer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.xdebugger.XDebuggerBundle;
import javax.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
abstract class DebuggerTreeWithHistoryContainer<D> {
  private static final Logger LOG = Logger.getInstance(DebuggerTreeWithHistoryContainer.class);
  private static final int HISTORY_SIZE = 11;
  private final List<D> myHistory = new ArrayList<D>();
  private int myCurrentIndex = -1;
  protected final DebuggerTreeCreator<D> myTreeCreator;
  @Nonnull
  protected final Project myProject;

  protected DebuggerTreeWithHistoryContainer(@Nonnull D initialItem, @Nonnull DebuggerTreeCreator<D> creator, @Nonnull Project project) {
    myTreeCreator = creator;
    myProject = project;
    myHistory.add(initialItem);
  }

  protected JPanel createMainPanel(Tree tree) {
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.add(ScrollPaneFactory.createScrollPane(tree), BorderLayout.CENTER);
    mainPanel.add(createToolbar(mainPanel, tree), BorderLayout.NORTH);
    return mainPanel;
  }

  private void updateTree() {
    D item = myHistory.get(myCurrentIndex);
    updateTree(item);
  }

  protected void updateTree(@Nonnull D selectedItem) {
    updateContainer(myTreeCreator.createTree(selectedItem), myTreeCreator.getTitle(selectedItem));
  }

  protected abstract void updateContainer(Tree tree, String title);

  protected void addToHistory(final D item) {
    if (myCurrentIndex < HISTORY_SIZE) {
      if (myCurrentIndex != -1) {
        myCurrentIndex += 1;
      } else {
        myCurrentIndex = 1;
      }
      myHistory.add(myCurrentIndex, item);
    }
  }

  private JComponent createToolbar(JPanel parent, Tree tree) {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new SetAsRootAction(tree));

    AnAction back = new GoBackwardAction();
    back.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_MASK)), parent);
    group.add(back);

    AnAction forward = new GoForwardAction();
    forward.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_MASK)), parent);
    group.add(forward);

    return ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true).getComponent();
  }

  private class GoForwardAction extends AnAction {
    public GoForwardAction() {
      super(CodeInsightBundle.message("quick.definition.forward"), null, AllIcons.Actions.Forward);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      if (myHistory.size() > 1 && myCurrentIndex < myHistory.size() - 1){
        myCurrentIndex ++;
        updateTree();
      }
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(myHistory.size() > 1 && myCurrentIndex < myHistory.size() - 1);
    }
  }

  private class GoBackwardAction extends AnAction {
    public GoBackwardAction() {
      super(CodeInsightBundle.message("quick.definition.back"), null, AllIcons.Actions.Back);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      if (myHistory.size() > 1 && myCurrentIndex > 0) {
        myCurrentIndex--;
        updateTree();
      }
    }


    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(myHistory.size() > 1 && myCurrentIndex > 0);
    }
  }

  private class SetAsRootAction extends AnAction {
    private final Tree myTree;

    public SetAsRootAction(Tree tree) {
      super(XDebuggerBundle.message("xdebugger.popup.value.tree.set.root.action.tooltip"),
            XDebuggerBundle.message("xdebugger.popup.value.tree.set.root.action.tooltip"), AllIcons.Modules.DeleteContentFolder);
      myTree = tree;
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
      TreePath path = myTree.getSelectionPath();
      e.getPresentation().setEnabled(path != null && path.getPathCount() > 1);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      TreePath path = myTree.getSelectionPath();
      if (path != null) {
        Object node = path.getLastPathComponent();
        myTreeCreator.createDescriptorByNode(node, new ResultConsumer<D>() {
          @Override
          public void onSuccess(final D value) {
            if (value != null) {
              ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                  addToHistory(value);
                  updateTree(value);
                }
              });
            }
          }

          @Override
          public void onFailure(@Nonnull Throwable t) {
            LOG.debug(t);
          }
        });
      }
    }
  }

  protected static void registerTreeDisposable(Disposable disposable, Tree tree) {
    if (tree instanceof Disposable) {
      Disposer.register(disposable, (Disposable)tree);
    }
  }
}
