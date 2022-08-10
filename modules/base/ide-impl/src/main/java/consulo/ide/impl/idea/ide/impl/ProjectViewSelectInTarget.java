/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.impl;

import consulo.application.ApplicationManager;
import consulo.ide.impl.idea.ide.CompositeSelectInTarget;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPane;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewPaneImpl;
import consulo.ide.impl.idea.util.ObjectUtils;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiUtilCore;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.view.tree.SelectableTreeStructureProvider;
import consulo.project.ui.view.tree.TreeStructureProvider;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.concurrent.ActionCallback;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ProjectViewSelectInTarget extends SelectInTargetPsiWrapper implements CompositeSelectInTarget {
  private String mySubId;

  protected ProjectViewSelectInTarget(Project project) {
    super(project);
  }

  @Override
  protected final void select(final Object selector, final VirtualFile virtualFile, final boolean requestFocus) {
    select(myProject, selector, getMinorViewId(), mySubId, virtualFile, requestFocus);
  }

  @Nonnull
  public static ActionCallback select(@Nonnull Project project,
                                                              final Object toSelect,
                                                              @Nullable final String viewId,
                                                              @Nullable final String subviewId,
                                                              final VirtualFile virtualFile,
                                                              final boolean requestFocus) {
    final ActionCallback result = new ActionCallback();

    final ProjectView projectView = ProjectView.getInstance(project);


    ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
    final ToolWindow projectViewToolWindow = windowManager.getToolWindow(ToolWindowId.PROJECT_VIEW);
    final Runnable runnable = () -> {
      Runnable r = () -> projectView.selectCB(toSelect, virtualFile, requestFocus).notify(result);
      projectView.changeViewCB(ObjectUtils.chooseNotNull(viewId, ProjectViewPaneImpl.ID), subviewId).doWhenProcessed(r);
    };

    if (requestFocus) {
      projectViewToolWindow.activate(runnable, true);
    }
    else {
      projectViewToolWindow.show(runnable);
    }

    return result;
  }

  @Override
  @Nonnull
  public Collection<SelectInTarget> getSubTargets(@Nonnull SelectInContext context) {
    List<SelectInTarget> result = new ArrayList<>();
    ProjectViewPane pane = ProjectView.getInstance(myProject).getProjectViewPaneById(getMinorViewId());
    int index = 0;
    for (String subId : pane.getSubIds()) {
      result.add(new ProjectSubViewSelectInTarget(this, subId, index++));
    }
    return result;
  }

  public boolean isSubIdSelectable(String subId, SelectInContext context) {
    return false;
  }

  @Override
  protected boolean canSelect(PsiFileSystemItem file) {
    return true;
  }

  public String getSubIdPresentableName(String subId) {
    ProjectViewPane pane = ProjectView.getInstance(myProject).getProjectViewPaneById(getMinorViewId());
    return pane.getPresentableSubIdName(subId);
  }

  @Override
  public void select(PsiElement element, final boolean requestFocus) {
    PsiElement toSelect = null;
    for (TreeStructureProvider provider : getProvidersDumbAware()) {
      if (provider instanceof SelectableTreeStructureProvider) {
        toSelect = ((SelectableTreeStructureProvider) provider).getTopLevelElement(element);
      }
      if (toSelect != null) break;
    }

    toSelect = findElementToSelect(element, toSelect);

    if (toSelect != null) {
      VirtualFile virtualFile = PsiUtilCore.getVirtualFile(toSelect);
      select(toSelect, virtualFile, requestFocus);
    }
  }

  private List<TreeStructureProvider> getProvidersDumbAware() {
    return DumbService.getInstance(myProject).filterByDumbAwareness(TreeStructureProvider.EP_NAME.getExtensionList(myProject));
  }

  @Override
  public final String getToolWindowId() {
    return ToolWindowId.PROJECT_VIEW;
  }

  @Override
  protected boolean canWorkWithCustomObjects() {
    return true;
  }

  public final void setSubId(String subId) {
    mySubId = subId;
  }

  public final String getSubId() {
    return mySubId;
  }
}