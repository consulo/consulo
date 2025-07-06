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

package consulo.execution.test.action;

import consulo.application.util.ListSelection;
import consulo.dataContext.DataContext;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffManager;
import consulo.diff.chain.DiffRequestChain;
import consulo.execution.test.AbstractTestProxy;
import consulo.execution.test.TestFrameworkRunningModel;
import consulo.execution.test.stacktrace.DiffHyperlink;
import consulo.execution.test.ui.TestTreeView;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIExAWTDataKey;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ViewAssertEqualsDiffAction extends AnAction implements TestTreeViewAction {
  @NonNls
  public static final String ACTION_ID = "openAssertEqualsDiff";

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    if (!openDiff(e.getDataContext(), null)) {
      final Component component = e.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
      Messages.showInfoMessage(component, "Comparison error was not found", "No Comparison Data Found");
    }
  }

  @RequiredUIAccess
  public static boolean openDiff(DataContext context, @Nullable DiffHyperlink currentHyperlink) {
    final AbstractTestProxy testProxy = context.getData(AbstractTestProxy.KEY);
    final Project project = context.getData(Project.KEY);
    if (testProxy != null) {
      DiffHyperlink diffViewerProvider = testProxy.getDiffViewerProvider();
      if (diffViewerProvider != null) {
        final List<DiffHyperlink> providers = collectAvailableProviders(context.getData(TestTreeView.MODEL_DATA_KEY));
        int index = currentHyperlink != null ? providers.indexOf(currentHyperlink) : -1;
        if (index == -1) index = providers.indexOf(diffViewerProvider);
        DiffRequestChain chain = TestDiffRequestProcessor.createRequestChain(project, ListSelection.createAt(providers, index));
        DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.DEFAULT);
        return true;
      }
    }
    if (currentHyperlink != null) {
      DiffRequestChain chain = TestDiffRequestProcessor.createRequestChain(project, ListSelection.createSingleton(currentHyperlink));
      DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.DEFAULT);
      return true;
    }
    return false;
  }

  private static List<DiffHyperlink> collectAvailableProviders(TestFrameworkRunningModel model) {
    final List<DiffHyperlink> providers = new ArrayList<>();
    if (model != null) {
      final AbstractTestProxy root = model.getRoot();
      final List<? extends AbstractTestProxy> allTests = root.getAllTests();
      for (AbstractTestProxy test : allTests) {
        if (test.isLeaf()) {
          providers.addAll(test.getDiffViewerProviders());
        }
      }
    }
    return providers;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final boolean enabled;
    final DataContext dataContext = e.getDataContext();
    if (dataContext.getData(Project.KEY) == null) {
      enabled = false;
    }
    else {
      final AbstractTestProxy test = dataContext.getData(AbstractTestProxy.KEY);
      if (test != null) {
        if (test.isLeaf()) {
          enabled = test.getDiffViewerProvider() != null;
        }
        else if (test.isDefect()) {
          enabled = true;
        }
        else {
          enabled = false;
        }
      }
      else {
        enabled = false;
      }
    }
    presentation.setEnabled(enabled);
    presentation.setVisible(enabled);
  }
}
