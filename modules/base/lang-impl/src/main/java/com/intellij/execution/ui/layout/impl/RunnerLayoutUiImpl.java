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

package com.intellij.execution.ui.layout.impl;

import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.LayoutAttractionPolicy;
import com.intellij.execution.ui.layout.LayoutStateDefaults;
import com.intellij.execution.ui.layout.LayoutViewOptions;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithActions;
import com.intellij.openapi.util.ActionCallback;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.ui.switcher.QuickActionProvider;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class RunnerLayoutUiImpl implements consulo.disposer.Disposable.Parent, RunnerLayoutUi, LayoutStateDefaults, LayoutViewOptions, DataProvider {
  private final RunnerLayout myLayout;
  private final RunnerContentUi myContentUI;

  private final ContentManager myViewsContentManager;
  public static final Key<String> CONTENT_TYPE = Key.create("ContentType");

  public RunnerLayoutUiImpl(@Nonnull Project project,
                            @Nonnull Disposable parent,
                            @Nonnull String runnerId,
                            @Nonnull String runnerTitle,
                            @Nonnull String sessionName) {
    myLayout = RunnerLayoutSettings.getInstance().getLayout(runnerId);
    consulo.disposer.Disposer.register(parent, this);

    myContentUI = new RunnerContentUi(project, this, ActionManager.getInstance(), IdeFocusManager.getInstance(project), myLayout,
                                      runnerTitle + " - " + sessionName);
    consulo.disposer.Disposer.register(this, myContentUI);

    myViewsContentManager = getContentFactory().createContentManager(myContentUI.getContentUI(), true, project);
    myViewsContentManager.addDataProvider(this);
    consulo.disposer.Disposer.register(this, myViewsContentManager);
  }

  @Override
  @Nonnull
  public LayoutViewOptions setTopToolbar(@Nonnull ActionGroup actions, @Nonnull String place) {
    myContentUI.setTopActions(actions, place);
    return this;
  }

  @Nonnull
  @Override
  public LayoutStateDefaults initTabDefaults(int id, String text, Image icon) {
    getLayout().setDefault(id, text, icon);
    return this;
  }

  @Nonnull
  @Override
  public LayoutStateDefaults initFocusContent(@Nonnull final String id, @Nonnull final String condition) {
    return initFocusContent(id, condition, new LayoutAttractionPolicy.FocusOnce());
  }

  @Nonnull
  @Override
  public LayoutStateDefaults initFocusContent(@Nonnull final String id, @Nonnull final String condition, @Nonnull final LayoutAttractionPolicy policy) {
    getLayout().setDefaultToFocus(id, condition, policy);
    return this;
  }

  @Override
  @Nonnull
  public Content addContent(@Nonnull Content content) {
    return addContent(content, false, -1, PlaceInGrid.center, false);
  }

  @Override
  @Nonnull
  public Content addContent(@Nonnull Content content, int defaultTabId, @Nonnull PlaceInGrid defaultPlace, boolean defaultIsMinimized) {
    return addContent(content, true, defaultTabId, defaultPlace, defaultIsMinimized);
  }

  public Content addContent(@Nonnull Content content, boolean applyDefaults, int defaultTabId, @Nonnull PlaceInGrid defaultPlace, boolean defaultIsMinimized) {
    final String id = content.getUserData(CONTENT_TYPE);

    assert id != null : "Content id is missing, use RunnerLayoutUi to create content instances";

    if (applyDefaults) {
      getLayout().setDefault(id, defaultTabId, defaultPlace, defaultIsMinimized);
    }

    getContentManager().addContent(content);
    return content;
  }

  @Override
  @Nonnull
  public Content createContent(@Nonnull String id, @Nonnull JComponent component, @Nonnull String displayName, @Nullable Image icon, @Nullable JComponent focusable) {
    return createContent(id, new ComponentWithActions.Impl(component), displayName, icon, focusable);
  }

  @Override
  @Nonnull
  public Content createContent(@Nonnull final String contentId, @Nonnull final ComponentWithActions withActions, @Nonnull final String displayName,
                               @Nullable final Image icon,
                               @Nullable final JComponent toFocus) {
    final Content content = getContentFactory().createContent(withActions.getComponent(), displayName, false);
    content.putUserData(CONTENT_TYPE, contentId);
    content.putUserData(ViewImpl.ID, contentId);
    content.setIcon(icon);
    if (toFocus != null) {
      content.setPreferredFocusableComponent(toFocus);
    }

    if (!withActions.isContentBuiltIn()) {
      content.setSearchComponent(withActions.getSearchComponent());
      content.setActions(withActions.getToolbarActions(), withActions.getToolbarPlace(), withActions.getToolbarContextComponent());
    }

    return content;
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return myViewsContentManager.getComponent();
  }

  private static ContentFactory getContentFactory() {
    return ContentFactory.getInstance();
  }

  public RunnerLayout getLayout() {
    return myLayout;
  }

  @Override
  public void updateActionsNow() {
    myContentUI.updateActionsImmediately();
  }

  @Override
  public void beforeTreeDispose() {
    myContentUI.saveUiState();
  }

  @Override
  public void dispose() {
  }

  @Override
  @Nonnull
  public ContentManager getContentManager() {
    return myViewsContentManager;
  }

  @Nonnull
  @Override
  public ActionCallback selectAndFocus(@Nullable final Content content, boolean requestFocus, final boolean forced) {
    return selectAndFocus(content, requestFocus, forced, false);
  }

  @Nonnull
  @Override
  public ActionCallback selectAndFocus(@Nullable final Content content, boolean requestFocus, final boolean forced, boolean implicit) {
    if (content == null) return ActionCallback.REJECTED;
    return getContentManager(content).setSelectedContent(content, requestFocus || shouldRequestFocus(), forced, implicit);
  }

  private ContentManager getContentManager(@Nonnull Content content) {
    return myContentUI.getContentManager(content);
  }

  private boolean shouldRequestFocus() {
    final Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    return focused != null && SwingUtilities.isDescendingFrom(focused, getContentManager().getComponent());
  }

  @Override
  public boolean removeContent(@Nullable Content content, final boolean dispose) {
    return content != null && getContentManager().removeContent(content, dispose);
  }

  @Override
  public boolean isToFocus(@Nonnull final Content content, @Nonnull final String condition) {
    final String id = content.getUserData(ViewImpl.ID);
    return getLayout().isToFocus(id, condition);
  }

  @Nonnull
  @Override
  public LayoutViewOptions setToFocus(@Nullable final Content content, @Nonnull final String condition) {
    getLayout().setToFocus(content != null ? content.getUserData(ViewImpl.ID) : null, condition);
    return this;
  }

  @Override
  public void attractBy(@Nonnull final String condition) {
    myContentUI.attractByCondition(condition, true);
  }

  @Override
  public void clearAttractionBy(@Nonnull final String condition) {
    myContentUI.clearAttractionByCondition(condition, true);
  }

  public void removeContent(@Nonnull String id, final boolean dispose) {
    final Content content = findContent(id);
    if (content != null) {
      getContentManager().removeContent(content, dispose);
    }
  }

  @Override
  public AnAction getLayoutActions() {
    return myContentUI.getLayoutActions();
  }

  @Nonnull
  @Override
  public AnAction[] getLayoutActionsList() {
    final ActionGroup group = (ActionGroup)getLayoutActions();
    return group.getChildren(null);
  }

  @Nonnull
  @Override
  public LayoutViewOptions setTabPopupActions(@Nonnull ActionGroup group) {
    myContentUI.setTabPopupActions(group);
    return this;
  }

  @Nonnull
  @Override
  public LayoutViewOptions setLeftToolbar(@Nonnull final ActionGroup leftToolbar, @Nonnull final String place) {
    myContentUI.setLeftToolbar(leftToolbar, place);
    return this;
  }

  @Override
  @Nullable
  public Content findContent(@Nonnull final String key) {
    return myContentUI.findContent(key);
  }

  @Nonnull
  @Override
  public RunnerLayoutUi addListener(@Nonnull final ContentManagerListener listener, @Nonnull final Disposable parent) {
    final ContentManager mgr = getContentManager();
    mgr.addContentManagerListener(listener);
    Disposer.register(parent, () -> mgr.removeContentManagerListener(listener));
    return this;
  }

  @Override
  public void removeListener(@Nonnull final ContentManagerListener listener) {
    getContentManager().removeContentManagerListener(listener);
  }

  @Override
  public void setBouncing(@Nonnull final Content content, final boolean activate) {
    myContentUI.processBounce(content, activate);
  }


  @Override
  public boolean isDisposed() {
    return getContentManager().isDisposed();
  }

  @Override
  @Nonnull
  public LayoutViewOptions setMinimizeActionEnabled(final boolean enabled) {
    myContentUI.setMinimizeActionEnabled(enabled);
    return this;
  }

  public LayoutViewOptions setToDisposeRemoveContent(boolean toDispose) {
    myContentUI.setToDisposeRemovedContent(toDispose);
    return this;
  }

  @Override
  @Nonnull
  public LayoutViewOptions setMoveToGridActionEnabled(final boolean enabled) {
    myContentUI.setMovetoGridActionEnabled(enabled);
    return this;
  }

  @Override
  @Nonnull
  public LayoutViewOptions setAttractionPolicy(@Nonnull final String contentId, final LayoutAttractionPolicy policy) {
    myContentUI.setPolicy(contentId, policy);
    return this;
  }

  @Nonnull
  @Override
  public LayoutViewOptions setConditionAttractionPolicy(@Nonnull final String condition, final LayoutAttractionPolicy policy) {
    myContentUI.setConditionPolicy(condition, policy);
    return this;
  }

  @Override
  @Nonnull
  public LayoutStateDefaults getDefaults() {
    return this;
  }

  @Override
  @Nonnull
  public LayoutViewOptions getOptions() {
    return this;
  }

  @Nonnull
  @Override
  public LayoutViewOptions setAdditionalFocusActions(@Nonnull final ActionGroup group) {
    myContentUI.setAdditionalFocusActions(group);
    return this;
  }

  @Override
  public AnAction getSettingsActions() {
    return myContentUI.getSettingsActions();
  }

  @Nonnull
  @Override
  public AnAction[] getSettingsActionsList() {
    final ActionGroup group = (ActionGroup)getSettingsActions();
    return group.getChildren(null);
  }

  @Nonnull
  @Override
  public Content[] getContents() {
    Content[] contents = new Content[getContentManager().getContentCount()];
    for (int i = 0; i < contents.length; i++) {
      contents[i] = getContentManager().getContent(i);
    }
    return contents;
  }

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key dataId) {
    if (QuickActionProvider.KEY == dataId || RunnerContentUi.KEY == dataId) {
      return myContentUI;
    }
    return null;
  }
}
