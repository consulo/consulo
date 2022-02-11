// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation;

import consulo.application.AllIcons;
import consulo.dataContext.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import consulo.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.project.ui.wm.*;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.content.*;
import consulo.application.ui.awt.UIUtil;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.update.Activatable;
import consulo.ui.ex.update.UiNotifyConnector;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.Rectangle2D;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import javax.annotation.Nonnull;
import javax.swing.*;

public abstract class DockablePopupManager<T extends JComponent & Disposable> {
  protected ToolWindow myToolWindow;
  private Runnable myAutoUpdateRequest;
  @Nonnull
  protected final Project myProject;

  public DockablePopupManager(@Nonnull Project project) {
    myProject = project;
  }

  protected abstract String getShowInToolWindowProperty();

  protected abstract String getAutoUpdateEnabledProperty();

  protected boolean getAutoUpdateDefault() {
    return false;
  }

  protected abstract String getAutoUpdateTitle();

  protected abstract String getRestorePopupDescription();

  protected abstract String getAutoUpdateDescription();

  protected abstract T createComponent();

  protected abstract void doUpdateComponent(PsiElement element, PsiElement originalElement, T component);

  protected void doUpdateComponent(Editor editor, PsiFile psiFile, boolean requestFocus) {
    doUpdateComponent(editor, psiFile);
  }

  protected abstract void doUpdateComponent(Editor editor, PsiFile psiFile);

  protected abstract void doUpdateComponent(@Nonnull PsiElement element);

  protected abstract String getTitle(PsiElement element);

  protected abstract String getToolwindowId();

  public Content recreateToolWindow(PsiElement element, PsiElement originalElement) {
    if (myToolWindow == null) {
      createToolWindow(element, originalElement);
      return null;
    }

    final Content content = myToolWindow.getContentManager().getSelectedContent();
    if (content == null || !myToolWindow.isVisible()) {
      restorePopupBehavior();
      createToolWindow(element, originalElement);
      return null;
    }
    return content;
  }

  public void createToolWindow(final PsiElement element, PsiElement originalElement) {
    assert myToolWindow == null;

    final T component = createComponent();

    final ToolWindowManagerEx toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(myProject);
    final ToolWindow toolWindow = toolWindowManagerEx.getToolWindow(getToolwindowId());
    myToolWindow = toolWindow == null ? toolWindowManagerEx.registerToolWindow(getToolwindowId(), true, ToolWindowAnchor.RIGHT, myProject) : toolWindow;
    myToolWindow.setIcon(AllIcons.Toolwindows.Documentation);

    myToolWindow.setAvailable(true, null);
    myToolWindow.setToHideOnEmptyContent(false);

    setToolwindowDefaultState();

    installComponentActions(myToolWindow, component);

    final ContentManager contentManager = myToolWindow.getContentManager();
    final ContentFactory contentFactory = ContentFactory.getInstance();
    final Content content = contentFactory.createContent(component, getTitle(element), false);
    contentManager.addContent(content);

    contentManager.addContentManagerListener(new ContentManagerAdapter() {
      @Override
      public void contentRemoved(@Nonnull ContentManagerEvent event) {
        restorePopupBehavior();
      }
    });

    new UiNotifyConnector(component, new Activatable() {
      @Override
      public void showNotify() {
        restartAutoUpdate(PropertiesComponent.getInstance().getBoolean(getAutoUpdateEnabledProperty(), getAutoUpdateDefault()));
      }

      @Override
      public void hideNotify() {
        restartAutoUpdate(false);
      }
    });

    myToolWindow.show(null);
    PropertiesComponent.getInstance().setValue(getShowInToolWindowProperty(), Boolean.TRUE.toString());
    restartAutoUpdate(PropertiesComponent.getInstance().getBoolean(getAutoUpdateEnabledProperty(), true));
    doUpdateComponent(element, originalElement, component);
  }

  protected void installComponentActions(ToolWindow toolWindow, T component) {
    ((ToolWindowEx)myToolWindow).setAdditionalGearActions(new DefaultActionGroup(createActions()));
  }

  protected void setToolwindowDefaultState() {
    final Rectangle2D rectangle = WindowManager.getInstance().getIdeFrame(myProject).suggestChildFrameBounds();
    myToolWindow.setDefaultState(ToolWindowAnchor.RIGHT, ToolWindowType.FLOATING, rectangle);
  }

  protected AnAction[] createActions() {
    ToggleAction toggleAutoUpdateAction = new ToggleAction(getAutoUpdateTitle(), getAutoUpdateDescription(), AllIcons.General.AutoscrollFromSource) {
      @Override
      public boolean isSelected(@Nonnull AnActionEvent e) {
        return PropertiesComponent.getInstance().getBoolean(getAutoUpdateEnabledProperty(), getAutoUpdateDefault());
      }

      @Override
      public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        PropertiesComponent.getInstance().setValue(getAutoUpdateEnabledProperty(), state, getAutoUpdateDefault());
        restartAutoUpdate(state);
      }
    };
    return new AnAction[]{createRestorePopupAction(), toggleAutoUpdateAction};
  }

  @Nonnull
  protected AnAction createRestorePopupAction() {
    return new DumbAwareAction("Open as Popup", getRestorePopupDescription(), null) {
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        restorePopupBehavior();
      }
    };
  }

  void restartAutoUpdate(final boolean state) {
    if (state && myToolWindow != null) {
      if (myAutoUpdateRequest == null) {
        myAutoUpdateRequest = this::updateComponent;

        UIUtil.invokeLaterIfNeeded(() -> IdeEventQueue.getInstance().addIdleListener(myAutoUpdateRequest, 500));
      }
    }
    else {
      if (myAutoUpdateRequest != null) {
        IdeEventQueue.getInstance().removeIdleListener(myAutoUpdateRequest);
        myAutoUpdateRequest = null;
      }
    }
  }

  public void updateComponent() {
    updateComponent(false);
  }

  public void updateComponent(boolean requestFocus) {
    if (myProject.isDisposed()) return;

    DataManager.getInstance().getDataContextFromFocusAsync().onSuccess(dataContext -> {
      if (!myProject.isOpen()) return;
      updateComponentInner(dataContext, requestFocus);
    });
  }

  private void updateComponentInner(@Nonnull DataContext dataContext, boolean requestFocus) {
    if (dataContext.getData(CommonDataKeys.PROJECT) != myProject) {
      return;
    }

    final Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    if (editor == null) {
      PsiElement element = dataContext.getData(CommonDataKeys.PSI_ELEMENT);
      if (element != null) {
        doUpdateComponent(element);
      }
      return;
    }

    PsiDocumentManager.getInstance(myProject).performLaterWhenAllCommitted(() -> {
      if (editor.isDisposed()) return;

      PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, myProject);
      Editor injectedEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file);
      PsiFile injectedFile = injectedEditor != null ? PsiUtilBase.getPsiFileInEditor(injectedEditor, myProject) : null;
      if (injectedFile != null) {
        doUpdateComponent(injectedEditor, injectedFile, requestFocus);
      }
      else if (file != null) {
        doUpdateComponent(editor, file, requestFocus);
      }
    });
  }


  public void restorePopupBehavior() {
    if (myToolWindow != null) {
      PropertiesComponent.getInstance().setValue(getShowInToolWindowProperty(), Boolean.FALSE.toString());
      ToolWindowManagerEx toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(myProject);
      toolWindowManagerEx.hideToolWindow(getToolwindowId(), false);
      toolWindowManagerEx.unregisterToolWindow(getToolwindowId());
      Disposer.dispose(myToolWindow.getContentManager());
      myToolWindow = null;
      restartAutoUpdate(false);
    }
  }

  public boolean hasActiveDockedDocWindow() {
    return myToolWindow != null && myToolWindow.isVisible();
  }
}
