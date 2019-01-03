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
package com.intellij.diff.merge;

import com.intellij.diff.DiffManagerEx;
import com.intellij.diff.actions.impl.NextDifferenceAction;
import com.intellij.diff.actions.impl.PrevDifferenceAction;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.tools.util.PrevNextDifferenceIterable;
import com.intellij.diff.util.DiffPlaces;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.diff.util.DiffUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.impl.DataManagerImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.BooleanGetter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ex.IdeFocusTraversalPolicy;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

// TODO: support merge request chains
// idea - to keep in memory all viewers that were modified (so binary conflict is not the case and OOM shouldn't be too often)
// suspend() / resume() methods for viewers? To not interfere with MergeRequest lifecycle: single request -> single viewer -> single applyResult()
public abstract class MergeRequestProcessor implements Disposable {
  private static final Logger LOG = Logger.getInstance(MergeRequestProcessor.class);

  private boolean myDisposed;

  @javax.annotation.Nullable
  private final Project myProject;
  @Nonnull
  private final MergeContext myContext;

  @Nonnull
  private final List<MergeTool> myAvailableTools;

  @Nonnull
  private final JPanel myPanel;
  @Nonnull
  private final MyPanel myMainPanel;
  @Nonnull
  private final Wrapper myContentPanel;
  @Nonnull
  private final Wrapper myToolbarPanel;
  @Nonnull
  private final Wrapper myToolbarStatusPanel;

  @Nonnull
  private final MergeRequest myRequest;

  @Nonnull
  private MergeTool.MergeViewer myViewer;
  @javax.annotation.Nullable
  private BooleanGetter myCloseHandler;
  @javax.annotation.Nullable
  private BottomActions myBottomActions;
  private boolean myConflictResolved = false;

  public MergeRequestProcessor(@javax.annotation.Nullable Project project, @Nonnull MergeRequest request) {
    myProject = project;
    myRequest = request;

    myContext = new MyDiffContext();
    myContext.putUserData(DiffUserDataKeys.PLACE, DiffPlaces.MERGE);

    myAvailableTools = DiffManagerEx.getInstance().getMergeTools();

    myMainPanel = new MyPanel();
    myContentPanel = new Wrapper();
    myToolbarPanel = new Wrapper();
    myToolbarPanel.setFocusable(true);
    myToolbarStatusPanel = new Wrapper();

    myPanel = JBUI.Panels.simplePanel(myMainPanel);

    JPanel topPanel = JBUI.Panels.simplePanel(myToolbarPanel).addToRight(myToolbarStatusPanel);

    myMainPanel.add(topPanel, BorderLayout.NORTH);
    myMainPanel.add(myContentPanel, BorderLayout.CENTER);

    myMainPanel.setFocusTraversalPolicyProvider(true);
    myMainPanel.setFocusTraversalPolicy(new MyFocusTraversalPolicy());

    MergeTool.MergeViewer viewer;
    try {
      viewer = getFittedTool().createComponent(myContext, myRequest);
    }
    catch (Throwable e) {
      LOG.error(e);
      viewer = ErrorMergeTool.INSTANCE.createComponent(myContext, myRequest);
    }

    myViewer = viewer;
    updateBottomActions();
  }

  //
  // Update
  //

  @RequiredUIAccess
  public void init() {
    setTitle(myRequest.getTitle());
    initViewer();
  }

  @RequiredUIAccess
  private void initViewer() {
    myContentPanel.setContent(myViewer.getComponent());

    MergeTool.ToolbarComponents toolbarComponents = myViewer.init();

    buildToolbar(toolbarComponents.toolbarActions);
    myToolbarStatusPanel.setContent(toolbarComponents.statusPanel);
    myCloseHandler = toolbarComponents.closeHandler;
  }

  @RequiredUIAccess
  private void destroyViewer() {
    Disposer.dispose(myViewer);

    ActionUtil.clearActions(myMainPanel);

    myContentPanel.setContent(null);
    myToolbarPanel.setContent(null);
    myToolbarStatusPanel.setContent(null);
    myCloseHandler = null;
    myBottomActions = null;
  }

  private void updateBottomActions() {
    myBottomActions = new BottomActions();
    myBottomActions.applyLeft = myViewer.getResolveAction(MergeResult.LEFT);
    myBottomActions.applyRight = myViewer.getResolveAction(MergeResult.RIGHT);
    myBottomActions.resolveAction = myViewer.getResolveAction(MergeResult.RESOLVED);
    myBottomActions.cancelAction = myViewer.getResolveAction(MergeResult.CANCEL);
  }

  @Nonnull
  protected DefaultActionGroup collectToolbarActions(@Nullable List<AnAction> viewerActions) {
    DefaultActionGroup group = new DefaultActionGroup();

    List<AnAction> navigationActions = ContainerUtil.<AnAction>list(new MyPrevDifferenceAction(),
                                                                    new MyNextDifferenceAction());
    DiffUtil.addActionBlock(group, navigationActions);

    DiffUtil.addActionBlock(group, viewerActions);

    List<AnAction> requestContextActions = myRequest.getUserData(DiffUserDataKeys.CONTEXT_ACTIONS);
    DiffUtil.addActionBlock(group, requestContextActions);

    List<AnAction> contextActions = myContext.getUserData(DiffUserDataKeys.CONTEXT_ACTIONS);
    DiffUtil.addActionBlock(group, contextActions);

    return group;
  }

  protected void buildToolbar(@Nullable List<AnAction> viewerActions) {
    ActionGroup group = collectToolbarActions(viewerActions);
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.DIFF_TOOLBAR, group, true);

    DataManager.registerDataProvider(toolbar.getComponent(), myMainPanel);
    toolbar.setTargetComponent(toolbar.getComponent());

    myToolbarPanel.setContent(toolbar.getComponent());
    for (AnAction action : group.getChildren(null)) {
      DiffUtil.registerAction(action, myMainPanel);
    }
  }

  @Nonnull
  private MergeTool getFittedTool() {
    for (MergeTool tool : myAvailableTools) {
      try {
        if (tool.canShow(myContext, myRequest)) return tool;
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }

    return ErrorMergeTool.INSTANCE;
  }

  private void setTitle(@javax.annotation.Nullable String title) {
    if (title == null) title = "Merge";
    setWindowTitle(title);
  }

  @Override
  public void dispose() {
    if (myDisposed) return;
    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        if (myDisposed) return;
        myDisposed = true;

        onDispose();

        destroyViewer();
      }
    });
  }

  @RequiredUIAccess
  private void applyRequestResult(@Nonnull MergeResult result) {
    if (myConflictResolved) return;
    myConflictResolved = true;
    try {
      myRequest.applyResult(result);
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  @RequiredUIAccess
  private void reopenWithTool(@Nonnull MergeTool tool) {
    if (myConflictResolved) {
      LOG.warn("Can't reopen with " + tool + " - conflict already resolved");
      return;
    }

    if (!tool.canShow(myContext, myRequest)) {
      LOG.warn("Can't reopen with " + tool + " - " + myRequest);
      return;
    }

    MergeTool.MergeViewer newViewer;
    try {
      newViewer = tool.createComponent(myContext, myRequest);
    }
    catch (Throwable e) {
      LOG.error(e);
      return;
    }

    boolean wasFocused = isFocused();

    destroyViewer();
    myViewer = newViewer;
    updateBottomActions();
    rebuildSouthPanel();
    initViewer();

    if (wasFocused) requestFocusInternal();
  }

  //
  // Abstract
  //

  @RequiredUIAccess
  protected void onDispose() {
    applyRequestResult(MergeResult.CANCEL);
  }

  protected void setWindowTitle(@Nonnull String title) {
  }

  protected abstract void rebuildSouthPanel();

  public abstract void closeDialog();

  @javax.annotation.Nullable
  public <T> T getContextUserData(@Nonnull Key<T> key) {
    return myContext.getUserData(key);
  }

  public <T> void putContextUserData(@Nonnull Key<T> key, @javax.annotation.Nullable T value) {
    myContext.putUserData(key, value);
  }

  //
  // Getters
  //

  @Nonnull
  public JComponent getComponent() {
    return myPanel;
  }

  @javax.annotation.Nullable
  public JComponent getPreferredFocusedComponent() {
    JComponent component = myViewer.getPreferredFocusedComponent();
    return component != null ? component : myToolbarPanel.getTargetComponent();
  }

  @javax.annotation.Nullable
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  public MergeContext getContext() {
    return myContext;
  }

  @RequiredUIAccess
  public boolean checkCloseAction() {
    return myConflictResolved || myCloseHandler == null || myCloseHandler.get();
  }

  @Nonnull
  public BottomActions getBottomActions() {
    return myBottomActions != null ? myBottomActions : new BottomActions();
  }

  @javax.annotation.Nullable
  public String getHelpId() {
    return (String)myMainPanel.getData(PlatformDataKeys.HELP_ID);
  }

  //
  // Misc
  //

  public boolean isFocused() {
    return DiffUtil.isFocusedComponent(myProject, myPanel);
  }

  public void requestFocus() {
    DiffUtil.requestFocus(myProject, getPreferredFocusedComponent());
  }

  protected void requestFocusInternal() {
    JComponent component = getPreferredFocusedComponent();
    if (component != null) component.requestFocusInWindow();
  }

  //
  // Navigation
  //

  private static class MyNextDifferenceAction extends NextDifferenceAction {
    @Override
    public void update(@Nonnull AnActionEvent e) {
      if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
        e.getPresentation().setEnabled(true);
        return;
      }

      PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
      if (iterable != null && iterable.canGoNext()) {
        e.getPresentation().setEnabled(true);
        return;
      }

      e.getPresentation().setEnabled(false);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
      if (iterable != null && iterable.canGoNext()) {
        iterable.goNext();
      }
    }
  }

  private static class MyPrevDifferenceAction extends PrevDifferenceAction {
    @Override
    public void update(@Nonnull AnActionEvent e) {
      if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
        e.getPresentation().setEnabled(true);
        return;
      }

      PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
      if (iterable != null && iterable.canGoPrev()) {
        e.getPresentation().setEnabled(true);
        return;
      }

      e.getPresentation().setEnabled(false);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
      if (iterable != null && iterable.canGoPrev()) {
        iterable.goPrev();
      }
    }
  }

  //
  // Helpers
  //

  private class MyPanel extends JPanel implements DataProvider {
    public MyPanel() {
      super(new BorderLayout());
    }

    @javax.annotation.Nullable
    @Override
    public Object getData(@Nonnull Key<?> dataId) {
      Object data;

      DataProvider contentProvider = DataManagerImpl.getDataProviderEx(myContentPanel.getTargetComponent());
      if (contentProvider != null) {
        data = contentProvider.getData(dataId);
        if (data != null) return data;
      }

      if (CommonDataKeys.PROJECT == dataId) {
        return myProject;
      }
      else if (PlatformDataKeys.HELP_ID == dataId) {
        if (myRequest.getUserData(DiffUserDataKeys.HELP_ID) != null) {
          return myRequest.getUserData(DiffUserDataKeys.HELP_ID);
        }
        else {
          return "procedures.vcWithIDEA.commonVcsOps.integrateDiffs.resolveConflict";
        }
      }

      DataProvider requestProvider = myRequest.getUserData(DiffUserDataKeys.DATA_PROVIDER);
      if (requestProvider != null) {
        data = requestProvider.getData(dataId);
        if (data != null) return data;
      }

      DataProvider contextProvider = myContext.getUserData(DiffUserDataKeys.DATA_PROVIDER);
      if (contextProvider != null) {
        data = contextProvider.getData(dataId);
        if (data != null) return data;
      }
      return null;
    }
  }

  private class MyFocusTraversalPolicy extends IdeFocusTraversalPolicy {
    @Override
    public final Component getDefaultComponentImpl(final Container focusCycleRoot) {
      JComponent component = MergeRequestProcessor.this.getPreferredFocusedComponent();
      if (component == null) return null;
      return IdeFocusTraversalPolicy.getPreferredFocusedComponent(component, this);
    }
  }

  private class MyDiffContext extends MergeContextEx {
    @javax.annotation.Nullable
    @Override
    public Project getProject() {
      return MergeRequestProcessor.this.getProject();
    }

    @Override
    public boolean isFocused() {
      return MergeRequestProcessor.this.isFocused();
    }

    @Override
    public void requestFocus() {
      MergeRequestProcessor.this.requestFocusInternal();
    }

    @Override
    public void finishMerge(@Nonnull MergeResult result) {
      applyRequestResult(result);
      MergeRequestProcessor.this.closeDialog();
    }

    @Override
    @RequiredUIAccess
    public void reopenWithTool(@Nonnull MergeTool tool) {
      MergeRequestProcessor.this.reopenWithTool(tool);
    }
  }

  public static class BottomActions {
    @javax.annotation.Nullable
    public Action applyLeft;
    @javax.annotation.Nullable
    public Action applyRight;
    @javax.annotation.Nullable
    public Action resolveAction;
    @javax.annotation.Nullable
    public Action cancelAction;
  }
}
