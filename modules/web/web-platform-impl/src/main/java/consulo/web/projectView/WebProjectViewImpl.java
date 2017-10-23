/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.projectView;

import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiElement;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import consulo.ide.projectView.ProjectViewEx;
import consulo.ui.Components;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 23-Oct-17
 */
public class WebProjectViewImpl implements ProjectViewEx {
  @Override
  public void setupToolWindow(@NotNull ToolWindow toolWindow, boolean loadPaneExtensions) {
    Content content = ContentFactory.getInstance().createUIContent(Components.label("Project View"), "Project", true);

    toolWindow.getContentManager().addContent(content);
  }

  @Override
  public void select(Object element, VirtualFile file, boolean requestFocus) {

  }

  @Override
  public ActionCallback selectCB(Object element, VirtualFile file, boolean requestFocus) {
    return null;
  }

  @Override
  public ActionCallback changeViewCB(@NotNull String viewId, String subId) {
    return null;
  }

  @Nullable
  @Override
  public PsiElement getParentOfCurrentSelection() {
    return null;
  }

  @Override
  public void changeView(String viewId) {

  }

  @Override
  public void changeView(String viewId, String subId) {

  }

  @Override
  public void changeView() {

  }

  @Override
  public void refresh() {

  }

  @Override
  public boolean isAutoscrollToSource(String paneId) {
    return false;
  }

  @Override
  public boolean isFlattenPackages(String paneId) {
    return false;
  }

  @Override
  public boolean isShowMembers(String paneId) {
    return false;
  }

  @Override
  public boolean isHideEmptyMiddlePackages(String paneId) {
    return false;
  }

  @Override
  public void setHideEmptyPackages(boolean hideEmptyPackages, String paneId) {

  }

  @Override
  public boolean isShowLibraryContents(String paneId) {
    return false;
  }

  @Override
  public void setShowLibraryContents(boolean showLibraryContents, String paneId) {

  }

  @Override
  public boolean isShowModules(String paneId) {
    return false;
  }

  @Override
  public void setShowModules(boolean showModules, String paneId) {

  }

  @Override
  public void addProjectPane(AbstractProjectViewPane pane) {

  }

  @Override
  public void removeProjectPane(AbstractProjectViewPane instance) {

  }

  @Override
  public AbstractProjectViewPane getProjectViewPaneById(String id) {
    return null;
  }

  @Override
  public boolean isAutoscrollFromSource(String paneId) {
    return false;
  }

  @Override
  public boolean isAbbreviatePackageNames(String paneId) {
    return false;
  }

  @Override
  public void setAbbreviatePackageNames(boolean abbreviatePackageNames, String paneId) {

  }

  @Override
  public String getCurrentViewId() {
    return null;
  }

  @Override
  public boolean isManualOrder(String paneId) {
    return false;
  }

  @Override
  public void setManualOrder(@NotNull String paneId, boolean enabled) {

  }

  @Override
  public void selectPsiElement(PsiElement element, boolean requestFocus) {

  }

  @Override
  public boolean isSortByType(String paneId) {
    return false;
  }

  @Override
  public void setSortByType(String paneId, boolean sortByType) {

  }

  @Override
  public AbstractProjectViewPane getCurrentProjectViewPane() {
    return null;
  }

  @Override
  public Collection<String> getPaneIds() {
    return null;
  }

  @Override
  public Collection<SelectInTarget> getSelectInTargets() {
    return null;
  }
}
