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

package com.intellij.ide.projectView;

import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.impl.AbstractProjectViewPSIPane;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public interface ProjectView {
  @Nonnull
  static ProjectView getInstance(Project project) {
    return ServiceManager.getService(project, ProjectView.class);
  }

  default void select(Object element, VirtualFile file, boolean requestFocus) {
    final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
    if (viewPane != null) {
      viewPane.select(element, file, requestFocus);
    }
  }

  @Nonnull
  default AsyncResult<Void> selectCB(Object element, VirtualFile file, boolean requestFocus){
    final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
    if (viewPane != null && viewPane instanceof AbstractProjectViewPSIPane) {
      return ((AbstractProjectViewPSIPane)viewPane).selectCB(element, file, requestFocus);
    }
    select(element, file, requestFocus);
    return AsyncResult.resolved();
  }

  @Nonnull
  AsyncResult<Void> changeViewCB(@Nonnull String viewId, String subId);

  @Nullable
  PsiElement getParentOfCurrentSelection();

  // show pane identified by id using default(or currently selected) subId
  void changeView(String viewId);

  void changeView(String viewId, String subId);

  void changeView();

  void refresh();

  boolean isAutoscrollToSource(String paneId);

  boolean isFlattenPackages(String paneId);

  boolean isShowMembers(String paneId);

  boolean isHideEmptyMiddlePackages(String paneId);

  void setHideEmptyPackages(boolean hideEmptyPackages, String paneId);

  boolean isShowLibraryContents(String paneId);

  void setShowLibraryContents(boolean showLibraryContents, String paneId);

  boolean isShowModules(String paneId);

  void setShowModules(boolean showModules, String paneId);

  void addProjectPane(final AbstractProjectViewPane pane);

  void removeProjectPane(AbstractProjectViewPane instance);

  AbstractProjectViewPane getProjectViewPaneById(String id);

  boolean isAutoscrollFromSource(String paneId);

  boolean isAbbreviatePackageNames(String paneId);

  void setAbbreviatePackageNames(boolean abbreviatePackageNames, String paneId);

  /**
   * e.g. {@link com.intellij.ide.projectView.impl.ProjectViewPane#ID}
   *
   * @see com.intellij.ide.projectView.impl.AbstractProjectViewPane#getId()
   */
  String getCurrentViewId();

  boolean isManualOrder(String paneId);

  void setManualOrder(@Nonnull String paneId, final boolean enabled);

  void selectPsiElement(PsiElement element, boolean requestFocus);

  boolean isSortByType(String paneId);

  void setSortByType(String paneId, final boolean sortByType);

  AbstractProjectViewPane getCurrentProjectViewPane();

  Collection<String> getPaneIds();

  @Nonnull
  Collection<SelectInTarget> getSelectInTargets();
}
